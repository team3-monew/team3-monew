import { Outlet } from "react-router";
import { PrivateHeader } from "@/shared/components/gnb";
import Footer from "@/shared/components/Footer";

export default function PrivateLayout() {
  return (
    <div className="min-h-dvh flex flex-col gap-[60px]">
      <div className="sticky top-0 z-30">
        <PrivateHeader />
      </div>

      <div className="flex-1 overflow-x-auto">
        <main className="flex-1 min-w-[1280px] px-4">
          <Outlet />
        </main>
      </div>

      <Footer />
    </div>
  );
}
