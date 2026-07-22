import { Toaster as Sonner } from "sonner";

type ToasterProps = React.ComponentProps<typeof Sonner>;

const Toaster = ({ ...props }: ToasterProps) => {
  return (
    <Sonner
      className="toaster group"
      closeButton
      position="top-right"
      offset={18}
      visibleToasts={4}
      toastOptions={{
        duration: 4200,
        classNames: {
          toast:
            "group toast group-[.toaster]:rounded-lg group-[.toaster]:border group-[.toaster]:border-hairline group-[.toaster]:bg-surface-elev group-[.toaster]:text-ink group-[.toaster]:shadow-xl group-[.toaster]:ring-1 group-[.toaster]:ring-black/5",
          success:
            "group-[.toaster]:border-[color-mix(in_oklab,var(--emerald-ink)_34%,var(--hairline))]",
          error:
            "group-[.toaster]:border-[color-mix(in_oklab,var(--destructive)_38%,var(--hairline))]",
          warning:
            "group-[.toaster]:border-[color-mix(in_oklab,var(--amber-ink)_42%,var(--hairline))]",
          info: "group-[.toaster]:border-[color-mix(in_oklab,var(--cyan)_38%,var(--hairline))]",
          title: "group-[.toast]:text-sm group-[.toast]:font-semibold group-[.toast]:text-ink",
          description:
            "group-[.toast]:text-xs group-[.toast]:leading-relaxed group-[.toast]:text-ink-soft",
          closeButton:
            "group-[.toast]:border-hairline group-[.toast]:bg-surface-elev group-[.toast]:text-ink-soft group-[.toast]:hover:text-ink",
          actionButton:
            "group-[.toast]:rounded-md group-[.toast]:bg-primary group-[.toast]:text-primary-foreground",
          cancelButton:
            "group-[.toast]:rounded-md group-[.toast]:bg-muted group-[.toast]:text-ink-soft",
        },
      }}
      {...props}
    />
  );
};

export { Toaster };
