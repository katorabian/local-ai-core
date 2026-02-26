import "./style.css";

import { marked } from "marked";
import DOMPurify from "dompurify";
import hljs from "highlight.js";
import "highlight.js/styles/github-dark.css";

type ChatSession = {
  id: string;
  model: string;
  createdAt: string;
};

type ChatMessage = {
  id: string;
  role: string;
  content: string;
  createdAt: string;
};

const API_BASE = "http://localhost:8080/api/v1";

/* ---------- markdown ---------- */

marked.setOptions({
  gfm: true,
  breaks: true,
});

function renderMarkdown(md: string): string {
  return DOMPurify.sanitize(marked.parse(md));
}

function highlightCode(container: HTMLElement) {
  container.querySelectorAll("pre code").forEach((block) => {
    hljs.highlightElement(block as HTMLElement);
  });
}

/* ---------- UI ---------- */

const app = document.getElementById("app")!;
app.innerHTML = `
  <div class="layout">
    <aside id="sessions" class="sidebar"></aside>

    <main class="chat">
      <div id="messages" class="messages"></div>

      <div class="input-wrapper">
        <div class="input-bar">
          <button class="icon-btn">＋</button>

          <input id="input" placeholder="Введите сообщение..." />

          <div class="right-actions">
            <button class="icon-btn">🎤</button>
            <button id="send" class="send-btn">↑</button>
          </div>
        </div>
      </div>
    </main>
  </div>
`;

const sessionsEl = document.getElementById("sessions")!;
const messagesEl = document.getElementById("messages")!;
const inputEl = document.getElementById("input") as HTMLInputElement;
const sendBtn = document.getElementById("send") as HTMLButtonElement;

let currentSessionId: string | null = null;

/* ---------- API ---------- */

async function loadSessions() {
  const res = await fetch(`${API_BASE}/chat/sessions`);
  const sessions: ChatSession[] = await res.json();

  sessionsEl.innerHTML = `
    <h3>Sessions</h3>
    <button id="newSession">+ New</button>
    <div style="margin-top:8px;">
      ${sessions
        .map(
          (s) => `
          <div data-id="${s.id}" style="cursor:pointer; margin-bottom:6px;">
            <b>${s.model}</b><br/>
            <small>${s.id}</small>
          </div>
        `
        )
        .join("")}
    </div>
  `;

  sessionsEl.querySelectorAll("[data-id]").forEach((el) => {
    el.addEventListener("click", () => {
      currentSessionId = (el as HTMLElement).dataset.id!;
      loadMessages(currentSessionId);
    });
  });

  document
    .getElementById("newSession")!
    .addEventListener("click", createSession);
}

async function createSession() {
  const res = await fetch(`${API_BASE}/chat/sessions`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: "",
  });

  const data = await res.json();
  currentSessionId = data.sessionId;
  await loadSessions();
  await loadMessages(currentSessionId);
}

async function loadMessages(sessionId: string) {
  const res = await fetch(`${API_BASE}/chat/sessions/${sessionId}/messages`);
  const messages: ChatMessage[] = await res.json();

  messagesEl.innerHTML = messages
    .map((m) => {
      if (m.role === "assistant") {
        return `
          <div class="message assistant">
            ${renderMarkdown(m.content)}
          </div>
        `;
      }

      return `
        <div class="message user">
          ${m.content}
        </div>
      `;
    })
    .join("");

  highlightCode(messagesEl);
  messagesEl.scrollTop = messagesEl.scrollHeight;
}

/* ---------- SSE ---------- */

async function sendMessage() {
  const text = inputEl.value.trim();
  if (!text) return;

  if (!currentSessionId) {
    const res = await fetch(`${API_BASE}/chat/sessions`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: "",
    });

    const data = await res.json();
    currentSessionId = data.sessionId;
    await loadSessions();
  }

  // user message (plain text)
  const userEl = document.createElement("div");
  userEl.className = "message user";
  userEl.textContent = text;
  messagesEl.appendChild(userEl);

  inputEl.value = "";

  // assistant placeholder
  const assistantEl = document.createElement("div");
  assistantEl.className = "message assistant";
  assistantEl.textContent = "thinking...";
  messagesEl.appendChild(assistantEl);

  let fullText = "";

  const response = await fetch(
    `${API_BASE}/chat/sessions/${currentSessionId}/stream`,
    {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        Accept: "text/event-stream",
      },
      body: JSON.stringify({ message: text }),
    }
  );

  if (!response.ok || !response.body) {
    assistantEl.textContent = "Ошибка соединения";
    return;
  }

  const reader = response.body.getReader();
  const decoder = new TextDecoder("utf-8");
  let buffer = "";

  while (true) {
    const { value, done } = await reader.read();
    if (done) break;

    buffer += decoder.decode(value, { stream: true });
    const events = buffer.split("\n\n");
    buffer = events.pop()!;

    for (const rawEvent of events) {
      let eventType = "message";
      let dataLine = "";

      for (const line of rawEvent.split("\n")) {
        if (line.startsWith("event:"))
          eventType = line.slice(6).trim();
        if (line.startsWith("data:"))
          dataLine += line.slice(5).trim();
      }

      if (!dataLine) continue;

      const data = JSON.parse(dataLine);

      if (data.message === "thinking") {
        assistantEl.textContent = "thinking...";
        continue;
      }

      if (data.text) {
        fullText += data.text;
        assistantEl.innerHTML = renderMarkdown(fullText);
        highlightCode(assistantEl);
        messagesEl.scrollTop = messagesEl.scrollHeight;
      }

      if (eventType === "done") {
        reader.cancel();
        return;
      }
    }
  }
}

/* ---------- events ---------- */

sendBtn.addEventListener("click", sendMessage);
inputEl.addEventListener("keydown", (e) => {
  if (e.key === "Enter") sendMessage();
});

loadSessions();