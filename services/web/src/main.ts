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

const app = document.getElementById("app")!;
app.innerHTML = `
  <div style="display: flex; height: 100vh;">
    <aside id="sessions" style="width: 280px; border-right: 1px solid #444; padding: 8px; overflow-y: auto;"></aside>

    <main style="flex: 1; display: flex; flex-direction: column;">
      <div id="messages" style="flex: 1; padding: 12px; overflow-y: auto;"></div>
      <div style="display:flex; padding:8px; border-top:1px solid #444;">
        <input id="input" style="flex:1; padding:8px;" placeholder="Type message..." />
        <button id="send" style="margin-left:8px;">Send</button>
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
    body: JSON.stringify({ model: "llama3.2:3b" }),
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
        `<div><b>${m.role}</b>: ${m.content}</div>`
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
      body: JSON.stringify({ model: "llama3.2:3b" }),
    });

    const data = await res.json();
    currentSessionId = data.sessionId;

    await loadSessions();
    await loadMessages(currentSessionId);
  }

  // временный user message (optimistic UI)
  const tempUserEl = document.createElement("div");
  tempUserEl.innerHTML = `<b>user</b>: ${text}`;
  tempUserEl.dataset.temp = "true";
  tempUserEl.dataset.role = "user";
  tempUserEl.dataset.content = text;
  messagesEl.appendChild(tempUserEl);

  // 2️⃣ очищаем поле ввода
  inputEl.value = "";

  // 3️⃣ подготавливаем место под ответ ассистента
  const assistantEl = document.createElement("div");
  assistantEl.innerHTML = `<b>assistant</b>: <i>thinking...</i>`;
  messagesEl.appendChild(assistantEl);
  let firstToken = true;

  // 4️⃣ запускаем SSE
  const eventSource = new EventSource(
    `${API_BASE}/chat/sessions/${currentSessionId}/stream?message=${encodeURIComponent(text)}`
  );

  eventSource.addEventListener("token", (e) => {
    const data = JSON.parse((e as MessageEvent).data);

    if (firstToken) {
      assistantEl.innerHTML = `<b>assistant</b>: `;
      firstToken = false;
    }

    assistantEl.innerHTML += data.text;
    messagesEl.scrollTop = messagesEl.scrollHeight;
  });

  eventSource.addEventListener("done", async () => {
    eventSource.close();

    eventSource.addEventListener("done", async () => {
      eventSource.close();

      const res = await fetch(
        `${API_BASE}/chat/sessions/${currentSessionId}/messages`
      );
      const messages: ChatMessage[] = await res.json();

      // аккуратный reconciliation
      messagesEl.innerHTML = messages
        .map(
          (m) =>
            `<div><b>${m.role}</b>: ${m.content}</div>`
        )
        .join("");
    });
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
