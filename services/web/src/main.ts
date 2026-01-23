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

import "./style.css";

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

  document.getElementById("newSession")!
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
    .map(
      (m) =>
        `<div class="message ${m.role}">
          ${m.content}
        </div>`
    )
    .join("");
}

/* ---------- SSE ---------- */

async function sendMessage() {
  const text = inputEl.value.trim();
  if (!text) return;

  // 1️⃣ если нет сессии — создаём автоматически
  if (!currentSessionId) {
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

  // временный user message (optimistic UI)
  const tempUserEl = document.createElement("div");
  tempUserEl.className = "message user";
  tempUserEl.textContent = text;
  tempUserEl.dataset.temp = "true";
  tempUserEl.dataset.role = "user";
  tempUserEl.dataset.content = text;
  messagesEl.appendChild(tempUserEl);

  // 2️⃣ очищаем поле ввода
  inputEl.value = "";

  // 3️⃣ подготавливаем место под ответ ассистента
  const assistantEl = document.createElement("div");
  assistantEl.className = "message assistant";
  assistantEl.textContent = "thinking...";
  messagesEl.appendChild(assistantEl);
  let firstToken = true;

  // 4️⃣ запускаем SSE
  const eventSource = new EventSource(
    `${API_BASE}/chat/sessions/${currentSessionId}/stream?message=${encodeURIComponent(text)}`
  );

  // ✅ ВАЖНО: бек всегда шлёт event: message
    eventSource.addEventListener("message", (e) => {
      const data = JSON.parse((e as MessageEvent).data);

      // Thinking
      if (data.message === "thinking") {
        assistantEl.textContent = "thinking...";
        return;
      }

      // Token stream
      if (data.text) {
        if (firstToken) {
          assistantEl.innerHTML = `<b>assistant</b>: `;
          firstToken = false;
        }

        assistantEl.innerHTML += data.text;
        messagesEl.scrollTop = messagesEl.scrollHeight;
        return;
      }

      // Error
      if (data.message) {
        assistantEl.textContent = `Ошибка: ${data.message}`;
        eventSource.close();
      }
    });

    // Завершение стрима
    eventSource.addEventListener("done", () => {
      eventSource.close();
    });

  eventSource.onerror = () => {
    eventSource.close();
  };
}

/* ---------- events ---------- */

sendBtn.addEventListener("click", sendMessage);
inputEl.addEventListener("keydown", (e) => {
  if (e.key === "Enter") sendMessage();
});

loadSessions();
