import Tag from "@/shared/components/Tag";

type SubscriptionCardProps = {
  name: string;
  keywords: string[];
  maxTags?: number;
};

export default function SubscriptionCard({
  name,
  keywords,
  maxTags = 8,
}: SubscriptionCardProps) {
  const safe = (keywords ?? []).filter(Boolean);
  const visible = safe.slice(0, maxTags);
  const overflow = safe.length > visible.length;

  return (
    <div className="flex flex-col w-[212px] gap-3 my-5 max-h-[168px] bg-tranparent">
      <p className="text-16-sb text-black">{name}</p>

      <div className="mt-3 flex flex-wrap gap-2">
        {visible.map((k) => (
          <Tag key={k} label={k} />
        ))}
        {overflow && <Tag label="…" />}
      </div>
    </div>
  );
}
