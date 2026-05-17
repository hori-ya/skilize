import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider } from './app/providers/AuthProvider';
import PrivateRoute from './shared/ui/PrivateRoute';
import AdminRoute from './shared/ui/AdminRoute';
import TlAdminRoute from './shared/ui/TlAdminRoute';
import ScrollToTopButton from './shared/ui/ScrollToTopButton';
import LoginPage from './features/auth/pages/LoginPage';
import ChangePasswordPage from './features/auth/pages/ChangePasswordPage';
import MyPasswordPage from './features/auth/pages/MyPasswordPage';
import DashboardPage from './features/inventory/pages/DashboardPage';
import InventoryPage from './features/inventory/pages/InventoryPage';
import ComparisonPage from './features/inventory/pages/ComparisonPage';
import GoalReviewPage from './features/inventory/pages/GoalReviewPage';
import GoalPage from './features/inventory/pages/GoalPage';
import InventoryHistoryPage from './features/inventory/pages/InventoryHistoryPage';
import TeamMemberListPage from './features/team/pages/TeamMemberListPage';
import MemberDetailPage from './features/team/pages/MemberDetailPage';
import AllUserListPage from './features/team/pages/AllUserListPage';
import FiscalYearMasterPage from './features/master/pages/FiscalYearMasterPage';
import SkillLevelMasterPage from './features/master/pages/SkillLevelMasterPage';
import ItSkillMasterPage from './features/master/pages/ItSkillMasterPage';
import QualificationMasterPage from './features/master/pages/QualificationMasterPage';
import AdSeminarMasterPage from './features/master/pages/AdSeminarMasterPage';
import UserMasterPage from './features/master/pages/UserMasterPage';

/**
 * アプリのルーティング定義。ルートガードは以下の3種類を使い分ける:
 * - PrivateRoute: ログイン必須（未ログイン → /login、初期PW → /change-password）
 * - TlAdminRoute: TL または ADMIN のみ
 * - AdminRoute: ADMIN のみ
 */
export default function App() {
  return (
    <AuthProvider>
      <BrowserRouter>
        <Routes>
          <Route path="/login" element={<LoginPage />} />
          <Route
            path="/change-password"
            element={
              <PrivateRoute requireInitialPassword>
                <ChangePasswordPage />
              </PrivateRoute>
            }
          />
          <Route
            path="/"
            element={
              <PrivateRoute>
                <DashboardPage />
              </PrivateRoute>
            }
          />
          <Route
            path="/inventory/:id"
            element={
              <PrivateRoute>
                <InventoryPage />
              </PrivateRoute>
            }
          />
          <Route
            path="/inventory/:id/comparison"
            element={
              <PrivateRoute>
                <ComparisonPage />
              </PrivateRoute>
            }
          />
          <Route
            path="/inventory/:id/goal-review"
            element={
              <PrivateRoute>
                <GoalReviewPage />
              </PrivateRoute>
            }
          />
          <Route
            path="/inventory/:id/goals"
            element={
              <PrivateRoute>
                <GoalPage />
              </PrivateRoute>
            }
          />
          <Route
            path="/inventory/history"
            element={
              <PrivateRoute>
                <InventoryHistoryPage />
              </PrivateRoute>
            }
          />
          <Route
            path="/team"
            element={
              <TlAdminRoute>
                <TeamMemberListPage />
              </TlAdminRoute>
            }
          />
          <Route
            path="/team/:userId"
            element={
              <TlAdminRoute>
                <MemberDetailPage />
              </TlAdminRoute>
            }
          />
          <Route
            path="/admin/users-inquiry"
            element={
              <AdminRoute>
                <AllUserListPage />
              </AdminRoute>
            }
          />
          <Route
            path="/master/fiscal-years"
            element={
              <AdminRoute>
                <FiscalYearMasterPage />
              </AdminRoute>
            }
          />
          <Route
            path="/master/skill-levels"
            element={
              <AdminRoute>
                <SkillLevelMasterPage />
              </AdminRoute>
            }
          />
          <Route
            path="/master/it-skills"
            element={
              <AdminRoute>
                <ItSkillMasterPage />
              </AdminRoute>
            }
          />
          <Route
            path="/master/qualifications"
            element={
              <AdminRoute>
                <QualificationMasterPage />
              </AdminRoute>
            }
          />
          <Route
            path="/master/ad-seminars"
            element={
              <AdminRoute>
                <AdSeminarMasterPage />
              </AdminRoute>
            }
          />
          <Route
            path="/master/users"
            element={
              <AdminRoute>
                <UserMasterPage />
              </AdminRoute>
            }
          />
          <Route
            path="/settings/password"
            element={
              <PrivateRoute>
                <MyPasswordPage />
              </PrivateRoute>
            }
          />
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
        <ScrollToTopButton />
      </BrowserRouter>
    </AuthProvider>
  );
}
