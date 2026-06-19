"use client";

import { useState } from "react";
import { useTranslations } from "next-intl";
import { ChevronDown } from "lucide-react";
import { cn } from "@/lib/utils";

// next-intl's getTranslations needs a server component to generate
// metadata, but the accordion's open/close state needs a client component.
// We keep this file a client component and skip a custom <title> — the
// root layout's default title still applies, same trade-off as other
// client-only pages in this app.
// next-intls getTranslations benötigt eine Server-Komponente für Metadaten,
// aber der Akkordeon-Zustand braucht eine Client-Komponente. Wir verzichten
// hier auf einen eigenen <title> — der Standard-Titel des Root-Layouts gilt.

const FAQ_KEYS = ["booking", "payment", "damage", "cancel", "verification", "accessories", "business", "support"];

/**
 * Public FAQ page (spec item #8) — common renter/owner questions in a
 * simple accordion, linked from the footer and the How-It-Works page.
 * Öffentliche FAQ-Seite — häufige Mieter-/Eigentümerfragen in einem
 * einfachen Akkordeon.
 */
export default function FaqPage() {
  const t = useTranslations("pages.faq");
  const [openKey, setOpenKey] = useState<string | null>(FAQ_KEYS[0]);

  return (
    <div className="mx-auto max-w-3xl px-4 sm:px-6 lg:px-8 py-16">
      <div className="text-center mb-10">
        <h1 className="text-3xl font-bold text-slate-900 mb-3">{t("title")}</h1>
        <p className="text-slate-600">{t("subtitle")}</p>
      </div>

      <div className="card divide-y divide-slate-100 overflow-hidden">
        {FAQ_KEYS.map((key) => {
          const isOpen = openKey === key;
          return (
            <div key={key}>
              <button
                type="button"
                onClick={() => setOpenKey(isOpen ? null : key)}
                className="w-full flex items-center justify-between gap-4 px-5 py-4 text-left"
              >
                <span className="font-medium text-slate-900">{t(`${key}Q`)}</span>
                <ChevronDown
                  size={18}
                  className={cn(
                    "shrink-0 text-slate-400 transition-transform",
                    isOpen && "rotate-180"
                  )}
                />
              </button>
              {isOpen && (
                <div className="px-5 pb-4 text-sm text-slate-600 leading-relaxed">
                  {t(`${key}A`)}
                </div>
              )}
            </div>
          );
        })}
      </div>
    </div>
  );
}
