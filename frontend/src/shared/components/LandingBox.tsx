type LandingBoxProps = {
  imageSrc: string;
  imageAlt: string;
  description: string;
  containerClassName?: string;
  descriptionClassName?: string;
};

export default function LandingBox({
  imageSrc,
  imageAlt,
  description,
  containerClassName,
  descriptionClassName,
}: LandingBoxProps) {
  return (
    <section
      className={[
        "flex flex-col items-center justify-center gap-4 p-4",
        "sm:w-[420px] h-[350px] rounded-2xl border-2",
        "w-[300px]",
        containerClassName ?? "",
      ].join(" ")}
      aria-label={imageAlt}
    >
      <img
        src={imageSrc}
        alt={imageAlt}
        className="w-full md:max-w-[370px] max-w-[320px] h-[265px] object-contain select-none"
        draggable={false}
      />
      <p className={["text-18-m", descriptionClassName ?? ""].join(" ")}>
        {description}
      </p>
    </section>
  );
}
