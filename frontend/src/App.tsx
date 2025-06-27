import React, { useEffect } from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate, Outlet, useLocation, useNavigate } from 'react-router-dom';
import { AuthProvider, useAuth } from './contexts/AuthContext';
import { SecurityProvider } from './contexts/SecurityContext';
import LoginPage from './pages/LoginPage';
import SignupPage from './pages/SignupPage';
import AdminLayout from './components/admin/AdminLayout';
import MainLayout from './layouts/MainLayout';
import AdminDashboardPage from './pages/admin/AdminDashboardPage';
import UserManagementPage from './pages/admin/UserManagementPage';
import EmailManagementPage from './pages/admin/EmailManagementPage';
import JobManagementPage from './pages/admin/JobManagementPage';
import AdminJobDetailsPage from './pages/admin/AdminJobDetailsPage';
import InterviewSkeletonManagementPage from './pages/admin/InterviewSkeletonManagementPage';
import InterviewAssignmentPage from './pages/admin/InterviewAssignmentPage';
import AdminChatPage from './pages/admin/AdminChatPage';
import ProtectedRoute from './components/auth/ProtectedRoute';
import { Role } from './types/user';
import EmailVerificationPage from './pages/EmailVerificationPage';
import ResetPasswordPage from './pages/ResetPasswordPage';
import DashboardPage from './pages/DashboardPage';
import ProfilePage from './pages/profile/ProfilePage';
import ProfileSettingsPage from './pages/profile/ProfileSettingsPage';
import SecuritySettingsPage from './pages/profile/SecuritySettingsPage';
import CandidateDashboardPage from './pages/candidate/CandidateDashboardPage';
import InterviewerDashboardPage from './pages/interviewer/InterviewerDashboardPage';
import InterviewDetailPage from './pages/interviewer/InterviewDetailPage';
import InterviewListPage from './pages/interviewer/InterviewListPage';
import LandingPage from './pages/LandingPage';
import JobsPage from './pages/JobsPage';
import JobDetailsPage from './pages/JobDetailsPage';
import JobApplicationPage from './pages/candidate/JobApplicationPage';
import ChatTestPage from './pages/ChatTestPage';
import { ToastContainer } from 'react-toastify';
import 'react-toastify/dist/ReactToastify.css';

// List of paths that should always be accessible, even when authenticated
const ALWAYS_ACCESSIBLE_PATHS = ['/reset-password', '/verify-email', '/dashboard'];

// URL Token Handler - This component processes tokens in the URL
// Separate from PublicRoute to ensure it runs on every route
const URLTokenHandler = () => {
  const location = useLocation();
  const { manuallySetToken } = useAuth();
  const navigate = useNavigate();

  useEffect(() => {
    const params = new URLSearchParams(location.search);
    const token = params.get('token');
    
    if (token) {
      console.log('URLTokenHandler - Processing token from URL');
      
      // Process the token
      manuallySetToken(token)
        .then(() => {
          console.log('URLTokenHandler - Token processed successfully');
          
          // Clean URL by removing token
          window.history.replaceState({}, document.title, location.pathname);
        })
        .catch(error => {
          console.error('URLTokenHandler - Failed to process token:', error);
        });
    }
  }, [location.search, manuallySetToken, navigate]);

  return null; // This component doesn't render anything
};

// Component to handle public routes (login, signup) with redirection for authenticated users
const PublicRoute: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const { user, token } = useAuth();
  const location = useLocation();
  const currentPath = location.pathname;
  
  // Check if the current path should always be accessible
  const isAlwaysAccessible = ALWAYS_ACCESSIBLE_PATHS.some(path => currentPath.startsWith(path));
  
  // If path should always be accessible, render children regardless of auth state
  if (isAlwaysAccessible) {
    return <>{children}</>;
  }

  // Otherwise, apply normal redirection logic for authenticated users
  if (user && token) {
    // Check if user is already on a valid path for their role
    const userRole = user.role;
    
    // If user is already on the correct role-based path, don't redirect
    if (
      (userRole === 'ADMIN' && currentPath.startsWith('/admin')) ||
      (userRole === 'INTERVIEWER' && currentPath.startsWith('/interviewer')) ||
      (userRole === 'HIRING_MANAGER' && currentPath.startsWith('/hiring-manager')) ||
      (userRole === 'CANDIDATE' && currentPath.startsWith('/candidate'))
    ) {
      // User is already on the correct path, no need to redirect
      return <>{children}</>;
    }
    
    // Otherwise redirect to dashboard which will handle role-based redirection
    return <Navigate to="/dashboard" replace />;
  }

  return <>{children}</>;
};

const App: React.FC = () => {
  return (
    <AuthProvider>
      <SecurityProvider>
        <Router>
          <URLTokenHandler />
          <ToastContainer 
            position="top-right"
            autoClose={5000}
            hideProgressBar={false}
            newestOnTop
            closeOnClick
            rtl={false}
            pauseOnFocusLoss
            draggable
            pauseOnHover
            theme="colored"
          />
          <Routes>
            {/* Public routes with redirection for authenticated users */}
            <Route path="/login" element={
              <PublicRoute>
                <LoginPage />
              </PublicRoute>
            } />
            <Route path="/signup" element={
              <PublicRoute>
                <SignupPage />
              </PublicRoute>
            } />
            <Route path="/verify-email" element={
              <PublicRoute>
                <EmailVerificationPage />
              </PublicRoute>
            } />
            <Route path="/reset-password" element={
              <PublicRoute>
                <ResetPasswordPage />
              </PublicRoute>
            } />
            
            {/* Dashboard route - needs to be accessible with token in URL */}
            <Route path="/dashboard" element={<DashboardPage />} />
            
            {/* Main authenticated routes using MainLayout */}
            <Route
              element={
                <ProtectedRoute allowedRoles={undefined}>
                  <MainLayout />
                </ProtectedRoute>
              }
            >
              {/* Profile Routes - accessible to all authenticated users */}
              <Route path="/profile">
                <Route index element={<ProfilePage />} />
                <Route path="settings" element={<ProfileSettingsPage />} />
                <Route path="security" element={<SecuritySettingsPage />} />
              </Route>

              {/* Candidate Routes */}
              <Route path="/candidate" element={<CandidateDashboardPage />} />

              {/* Interviewer Routes */}
              <Route
                path="/interviewer"
                element={
                  <ProtectedRoute allowedRoles={[Role.INTERVIEWER]}>
                    <InterviewerDashboardPage />
                  </ProtectedRoute>
                }
              />
              <Route
                path="/interviewer/interviews"
                element={
                  <ProtectedRoute allowedRoles={[Role.INTERVIEWER]}>
                    <InterviewListPage />
                  </ProtectedRoute>
                }
              />
              <Route
                path="/interviewer/interviews/:interviewId"
                element={
                  <ProtectedRoute allowedRoles={[Role.INTERVIEWER]}>
                    <InterviewDetailPage />
                  </ProtectedRoute>
                }
              />

              {/* Hiring Manager Routes */}
              <Route
                path="/hiring-manager"
                element={
                  <ProtectedRoute allowedRoles={[Role.HIRING_MANAGER]}>
                    <div>
                      <h1>Hiring Manager Dashboard (Coming Soon)</h1>
                    </div>
                  </ProtectedRoute>
                }
              />
            </Route>
            
            {/* Admin Routes */}
            <Route
              path="/admin"
              element={
                <ProtectedRoute allowedRoles={[Role.ADMIN]}>
                  <AdminLayout>
                    <Outlet />
                  </AdminLayout>
                </ProtectedRoute>
              }
            >
              <Route index element={<AdminDashboardPage />} />
              <Route path="users" element={<UserManagementPage />} />
              <Route path="emails" element={<EmailManagementPage />} />
              <Route path="jobs" element={<JobManagementPage />} />
              <Route path="jobs/:jobId" element={<AdminJobDetailsPage />} />
              <Route path="interview-skeletons" element={<InterviewSkeletonManagementPage />} />
              <Route path="interview-assignments" element={<InterviewAssignmentPage />} />
              <Route path="chat" element={<AdminChatPage />} />
            </Route>
            
            {/* Public routes */}
            <Route path="/" element={<LandingPage />} />
            <Route path="/jobs" element={<JobsPage />} />
            <Route path="/jobs/:id" element={<JobDetailsPage />} />
            <Route path="/apply/:id" element={<JobApplicationPage />} />
            <Route path="/chat-test" element={<ChatTestPage />} />
            
            {/* Catch-all route */}
            <Route path="*" element={<Navigate to="/" />} />
          </Routes>
        </Router>
      </SecurityProvider>
    </AuthProvider>
  );
};

export default App; 