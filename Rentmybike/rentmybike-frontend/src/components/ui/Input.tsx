import { forwardRef, InputHTMLAttributes } from "react";
import { cn } from "@/lib/utils";

interface InputProps extends InputHTMLAttributes<HTMLInputElement> {
  label?: string;
  error?: string;
  hint?: string;
}

/**
 * Reusable Input with optional label and error message.
 * Wiederverwendbares Input mit optionalem Label und Fehlermeldung.
 */
export const Input = forwardRef<HTMLInputElement, InputProps>(
  ({ label, error, hint, className, id, ...props }, ref) => {
    const inputId = id ?? label?.toLowerCase().replace(/\s+/g, "-");

    return (
      <div className="w-full">
        {label && (
          <label htmlFor={inputId} className="label">
            {label}
          </label>
        )}
        <input
          ref={ref}
          id={inputId}
          className={cn(
            "w-full rounded-xl border bg-white px-3 py-2 text-sm text-slate-900",
            "placeholder:text-slate-400 outline-none transition-colors",
            "focus:ring-2 focus:ring-brand-500 focus:border-brand-500",
            "disabled:cursor-not-allowed disabled:bg-slate-50 disabled:text-slate-500",
            error
              ? "border-red-400 focus:ring-red-400 focus:border-red-400"
              : "border-slate-300",
            className
          )}
          {...props}
        />
        {error && <p className="field-error">{error}</p>}
        {hint && !error && <p className="text-xs text-slate-500 mt-1">{hint}</p>}
      </div>
    );
  }
);
Input.displayName = "Input";
