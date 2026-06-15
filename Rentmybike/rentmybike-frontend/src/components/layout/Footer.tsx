import Link from "next/link";
import { useLocale } from "next-intl";
import { Bike } from "lucide-react";

export function Footer() {
  const locale = useLocale();
  const year = new Date().getFullYear();

  return (
    <footer className="border-t border-slate-200 bg-slate-50 mt-auto">
      <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8 py-8">
        <div className="flex flex-col md:flex-row items-center justify-between gap-4">
          <Link
            href={`/${locale}/`}
            className="flex items-center gap-2 font-bold text-brand-600"
          >
            <Bike size={20} />
            RentMyBike
          </Link>

          <p className="text-sm text-slate-500">
            © {year} RentMyBike. All rights reserved.
          </p>

          <div className="flex gap-4 text-sm text-slate-500">
            <Link href={`/${locale}/`} className="hover:text-slate-900 transition-colors">
              Home
            </Link>
            <Link href={`/${locale}/bikes`} className="hover:text-slate-900 transition-colors">
              Bikes
            </Link>
          </div>
        </div>
      </div>
    </footer>
  );
}
