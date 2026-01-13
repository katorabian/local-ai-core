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
    <aside id="sessions" style="width: 300px; border-right: 1px solid #444; padding: 12px; overflow-y: auto;"></aside>
    <main style="flex: 1; padding: 12px; overflow-y: auto;">
      <div id="messages"></div>
    </main>
  </div>
`;

const sessionsEl = document.getElementById("sessions")!;
const messagesEl = document.getElementById("messages")!;

async function loadSessions() {
  const res = await fetch(`${API_BASE}/chat/sessions`);
  const sessions: ChatSession[] = await res.json();

  sessionsEl.innerHTML = `
    <h3>Sessions</h3>
    ${sessions
      .map(
        (s) => `
          <div
            style="cursor:pointer; margin-bottom:8px;"
            data-id="${s.id}"
          >
            <b>${s.model}</b><br/>
            <small>${s.id}</small>
          </div>
        `
      )
      .join("")}
  `;

  sessionsEl.querySelectorAll("[data-id]").forEach((el) => {
    el.addEventListener("click", () => {
      const id = (el as HTMLElement).dataset.id!;
      loadMessages(id);
    });
  });
}

async function loadMessages(sessionId: string) {
  const res = await fetch(
    `${API_BASE}/chat/sessions/${sessionId}/messages`
  );
  const messages: ChatMessage[] = await res.json();

  messagesEl.innerHTML = `
    <h3>Messages</h3>
    ${messages
      .map(
        (m) => `
          <div style="margin-bottom:12px;">
            <b>${m.role}</b>: ${m.content}
          </div>
        `
      )
      .join("")}
  `;
}

loadSessions();
