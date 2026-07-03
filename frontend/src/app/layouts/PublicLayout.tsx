import { Outlet } from "react-router";
import { PublicHeader } from "@/shared/components/gnb";

export default function PublicLayout() {
  return (
    <div className="flex flex-col min-h-dvh">
      <PublicHeader />
      <main className="flex-1 flex items-center justify-center p-8">
        <Outlet />
      </main>
    </div>
  );
}
