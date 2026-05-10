import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider } from './contexts/AuthContext';
import PrivateRoute from './components/PrivateRoute';
import AdminRoute from './components/AdminRoute';
import LoginPage from './pages/LoginPage';
import ChangePasswordPage from './pages/ChangePasswordPage';
import DashboardPage from './pages/DashboardPage';
import InventoryPage from './pages/InventoryPage';
import ComparisonPage from './pages/ComparisonPage';
import GoalReviewPage from './pages/GoalReviewPage';
import GoalPage from './pages/GoalPage';
import InventoryHistoryPage from './pages/InventoryHistoryPage';
import TeamMemberListPage from './pages/TeamMemberListPage';
import MemberDetailPage from './pages/MemberDetailPage';
import AllUserListPage from './pages/AllUserListPage';
import TlAdminRoute from './components/TlAdminRoute';
import ScrollToTopButton from './components/ScrollToTopButton';
import FiscalYearMasterPage from './pages/master/FiscalYearMasterPage';
import SkillLevelMasterPage from './pages/master/SkillLevelMasterPage';
import ItSkillMasterPage from './pages/master/ItSkillMasterPage';
import QualificationMasterPage from './pages/master/QualificationMasterPage';
import AdSeminarMasterPage from './pages/master/AdSeminarMasterPage';
import UserMasterPage from './pages/master/UserMasterPage';

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
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
        <ScrollToTopButton />
      </BrowserRouter>
    </AuthProvider>
  );
}
