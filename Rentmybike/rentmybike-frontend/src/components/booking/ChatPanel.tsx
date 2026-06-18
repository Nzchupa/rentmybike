"use client";

import { useEffect, useRef, useState } from "react";
import { useTranslations } from "next-intl";
import { Client, type IMessage } from "@stomp/stompjs";
import SockJS from "sockjs-client";
import { Send } from "lucide-react";
import toast from "react-hot-toast";
import { chatApi } from "@/lib/api";
import { useAuthStore } from "@/store/auth.store";
import { Avatar } from "@/components/ui/Avatar";
import type { ChatMessageResponse } from "@/types";

interface ChatPanelProps {
  bookingId: string;
}

// The SockJS endpoint is registered at /ws on the same backend the REST API
// already talks to — SockJS negotiates the actual transport (WebSocket with
// XHR fallback) internally, so we just point it at an http(s) URL.
// Der SockJS-Endpunkt ist unter /ws auf demselben Backend registriert, mit
// dem die REST-API bereits spricht — SockJS verhandelt den tatsächlichen
// Transport (WebSocket mit XHR-Fallback) intern, daher zeigen wir einfach
// auf eine http(s)-URL.
const WS_URL = `${process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080"}/ws`;

/**
 * Real-time chat thread for a single booking, scoped to its renter and owner.
 * Echtzeit-Chat-Thread für eine einzelne Buchung, beschränkt auf Mieter und
 * Eigentümer.
 *
 * <p>History is loaded once via REST on mount; new messages arrive over a
 * STOMP/SockJS connection to {@code /topic/booking/{bookingId}} and are sent
 * via {@code /app/chat/{bookingId}/send}. The httpOnly JWT cookie that
 * already authenticates REST calls also authenticates the WebSocket
 * handshake automatically — no separate token handling needed here.
 * <p>Der Verlauf wird beim Mounten einmal per REST geladen; neue Nachrichten
 * kommen über eine STOMP/SockJS-Verbindung zu
 * {@code /topic/booking/{bookingId}} an und werden über
 * {@code /app/chat/{bookingId}/send} gesendet. Das httpOnly-JWT-Cookie, das
 * bereits REST-Aufrufe authentifiziert, authentifiziert auch automatisch den
 * WebSocket-Handshake — hier ist keine separate Token-Verwaltung nötig.
 */
export function ChatPanel({ bookingId }: ChatPanelProps) {
  const t = useTranslations("booking.chat");
  const { user } = useAuthStore();
  const [messages, setMessages] = useState<ChatMessageResponse[]>([]);
  const [loadingHistory, setLoadingHistory] = useState(true);
  const [connected, setConnected] = useState(false);
  const [input, setInput] = useState("");
  const clientRef = useRef<Client | null>(null);
  const scrollRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    let active = true;
    chatApi
      .getHistory(bookingId)
      .then((res) => {
        if (active) setMessages(res.data.data);
      })
      .catch(() => {
        if (active) toast.error(t("loadError"));
      })
      .finally(() => {
        if (active) setLoadingHistory(false);
      });
    return () => {
      active = false;
    };
  }, [bookingId, t]);

  useEffect(() => {
    const client = new Client({
      webSocketFactory: () => new SockJS(WS_URL, undefined, { transports: ["websocket"] }) as unknown as WebSocket,
      reconnectDelay: 3000,
      onConnect: () => {
        setConnected(true);
        client.subscribe(`/topic/booking/${bookingId}`, (msg: IMessage) => {
          const incoming = JSON.parse(msg.body) as ChatMessageResponse;
          setMessages((prev) => [...prev, incoming]);
        });
      },
      onDisconnect: () => setConnected(false),
      onStompError: () => setConnected(false),
      onWebSocketClose: () => setConnected(false),
    });

    client.activate();
    clientRef.current = client;

    return () => {
      client.deactivate();
      clientRef.current = null;
    };
  }, [bookingId]);

  useEffect(() => {
    scrollRef.current?.scrollTo({ top: scrollRef.current.scrollHeight, behavior: "smooth" });
  }, [messages]);

  function handleSend() {
    const content = input.trim();
    if (!content || !clientRef.current?.connected) return;

    clientRef.current.publish({
      destination: `/app/chat/${bookingId}/send`,
      body: JSON.stringify({ content }),
    });
    setInput("");
  }

  if (loadingHistory) {
    return <div className="h-64 animate-pulse bg-slate-100 rounded-xl" />;
  }

  return (
    <div className="flex flex-col h-72 border border-slate-100 rounded-xl overflow-hidden">
      <div ref={scrollRef} className="flex-1 overflow-y-auto p-3 space-y-2 bg-slate-50">
        {messages.length === 0 && (
          <p className="text-xs text-slate-400 text-center mt-8">{t("empty")}</p>
        )}
        {messages.map((m) => {
          const mine = m.senderId === user?.id;
          return (
            <div key={m.id} className={`flex items-end gap-2 ${mine ? "justify-end" : "justify-start"}`}>
              {!mine && <Avatar name={m.senderName} avatarUrl={m.senderAvatarUrl} size="sm" />}
              <div
                className={`max-w-[70%] rounded-2xl px-3 py-1.5 text-sm break-words ${
                  mine
                    ? "bg-brand-600 text-white"
                    : "bg-white text-slate-700 border border-slate-200"
                }`}
              >
                {m.content}
              </div>
            </div>
          );
        })}
      </div>

      <div className="flex items-center gap-2 p-2 border-t border-slate-100 bg-white">
        <input
          value={input}
          onChange={(e) => setInput(e.target.value)}
          onKeyDown={(e) => {
            if (e.key === "Enter") handleSend();
          }}
          placeholder={t("placeholder")}
          className="flex-1 text-sm rounded-lg border border-slate-200 px-3 py-1.5 focus:outline-none focus:ring-1 focus:ring-brand-500"
        />
        <button
          type="button"
          disabled={!connected || !input.trim()}
          onClick={handleSend}
          className="inline-flex items-center justify-center w-8 h-8 rounded-full bg-brand-600 text-white disabled:opacity-40 shrink-0"
          aria-label={t("send")}
        >
          <Send size={15} />
        </button>
      </div>
    </div>
  );
}
