type SkeletonProps = {
  width?: string;
  height?: string;
  rounded?: string;
  className?: string;
};

export default function Skeleton({
  width = "100%",
  height = "1rem",
  rounded = "rounded-md",
  className,
}: SkeletonProps) {
  const classes = ["animate-pulse", "bg-gray-200", rounded, className]
    .filter(Boolean)
    .join(" ");

  return <div className={classes} style={{ width, height }} />;
}
