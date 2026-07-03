import { Routes, Route } from "react-router";
import PrivateLayout from "@/app/layouts/PrivateLayout";
import PublicLayout from "@/app/layouts/PublicLayout";
import LandingPage from "@/app/routes/public/LandingPage";
import LoginPage from "@/app/routes/public/LoginPage";
import SignUpPage from "@/app/routes/public/SignUpPage";
import ArticlesPage from "@/app/routes/private/ArticlesPage";
import InterestsPage from "@/app/routes/private/InterestsPage";
import ActivitiesPage from "@/app/routes/private/ActivitiesPage";
import NotFound from "@/app/routes/public/not-found";
import { RequireAuth } from "@/features/auth/guards/RequireAuth";

function App() {
  return (
    <Routes>
      {/* 보호 라우트: 로그인 필요 */}
      <Route element={<RequireAuth />}>
        <Route element={<PrivateLayout />}>
          <Route path="/articles" element={<ArticlesPage />} />
          <Route path="/articles/:articleId" element={<ArticlesPage />} />
          <Route path="/interests" element={<InterestsPage />} />
          <Route path="/activities" element={<ActivitiesPage />} />
        </Route>
      </Route>

      {/* 게스트 전용 */}
      <Route element={<PublicLayout />}>
        <Route path="/" element={<LandingPage />} />
        <Route path="/login" element={<LoginPage />} />
        <Route path="/signup" element={<SignUpPage />} />
        <Route path="*" element={<NotFound />} />
      </Route>
    </Routes>
  );
}

export default App;
