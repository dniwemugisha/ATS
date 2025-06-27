import React, { useEffect, useState } from 'react';
import { useAuth } from '../contexts/AuthContext';
import { useNavigate } from 'react-router-dom';
import axiosInstance from '../utils/axios';
import { Role } from '../types/user';
import { getTargetRouteForUser } from '../utils/routeUtils';

const DashboardPage: React.FC = () => {
  console.log('DashboardPage - Rendering');
  const { user, setUser, setToken, setIsAuthenticated, token } = useAuth();
  const navigate = useNavigate();
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // Function to redirect based on role, preserving stored routes when possible
  const redirectBasedOnRole = (userRole: string) => {
    console.log('DashboardPage - Redirecting based on role:', userRole);
    
    // If role is empty or undefined, show error
    if (!userRole) {
      console.error('DashboardPage - Role is empty or undefined');
      setError('Unable to determine user role. Please log out and log in again.');
      setIsLoading(false);
      return;
    }
    
    // Get the current path to check if user is already on a valid path for their role
    const currentPath = window.location.pathname;
    console.log('DashboardPage - Current path:', currentPath);
    
    // Check if user is already on a valid path for their role
    const normalizedRole = userRole.replace('ROLE_', '').toUpperCase();
    let isOnValidPath = false;
    
    switch (normalizedRole) {
      case 'ADMIN':
        isOnValidPath = currentPath.startsWith('/admin');
        break;
      case 'INTERVIEWER':
        isOnValidPath = currentPath.startsWith('/interviewer');
        break;
      case 'HIRING_MANAGER':
        isOnValidPath = currentPath.startsWith('/hiring-manager');
        break;
      case 'CANDIDATE':
        isOnValidPath = currentPath.startsWith('/candidate');
        break;
    }
    
    if (isOnValidPath) {
      console.log('DashboardPage - User already on valid path, no redirect needed');
      setIsLoading(false);
      return;
    }
    
    // Get target route (either stored route or default dashboard)
    const targetPath = getTargetRouteForUser(userRole);
    console.log('DashboardPage - Redirecting to:', targetPath);
    
    if (targetPath === '/dashboard') {
      setError(`Unknown role: ${userRole}. Please contact support.`);
      setIsLoading(false);
      return;
    }
    
    navigate(targetPath, { replace: true });
  };

  useEffect(() => {
    console.log('DashboardPage - useEffect triggered');
    
    // Check if there's a stored route to restore
    const storedRoute = sessionStorage.getItem('lastVisitedRoute');
    console.log('DashboardPage - Stored route found:', storedRoute);
    
    if (user && storedRoute) {
      // Check if the stored route matches the user's role
      const userRole = user.role;
      let shouldRestoreRoute = false;
      
      if (userRole === Role.ADMIN && storedRoute.startsWith('/admin/')) {
        shouldRestoreRoute = true;
      } else if (userRole === Role.INTERVIEWER && storedRoute.startsWith('/interviewer/')) {
        shouldRestoreRoute = true;
      } else if (userRole === Role.HIRING_MANAGER && storedRoute.startsWith('/hiring-manager/')) {
        shouldRestoreRoute = true;
      } else if (userRole === Role.CANDIDATE && storedRoute.startsWith('/candidate/')) {
        shouldRestoreRoute = true;
      }
      
      if (shouldRestoreRoute) {
        console.log('DashboardPage - Restoring stored route:', storedRoute);
        sessionStorage.removeItem('lastVisitedRoute');
        navigate(storedRoute, { replace: true });
        return;
      } else {
        // Clear invalid stored route
        sessionStorage.removeItem('lastVisitedRoute');
      }
    }
    
    const checkUserAndRedirect = async () => {
      try {
        if (user) {
          console.log('DashboardPage - User found:', user);
          console.log('DashboardPage - User role:', user.role);
          // If user is already loaded, redirect based on role
          redirectBasedOnRole(user.role);
        } else if (token) {
          console.log('DashboardPage - No user but token found, fetching user data');
          // We have a token but no user, fetch user data
          try {
            const formattedToken = token.startsWith('Bearer ') ? token : `Bearer ${token}`;
            axiosInstance.defaults.headers.common['Authorization'] = formattedToken;
            
            const response = await axiosInstance.get('/auth/me');
            const userData = response.data;
            console.log('DashboardPage - Fetched user data:', userData);
            
            localStorage.setItem('user', JSON.stringify(userData));
            setUser(userData);
            
            // Redirect based on role after fetching user data
            redirectBasedOnRole(userData.role);
          } catch (error) {
            console.error('DashboardPage - Failed to fetch user data:', error);
            setError('Failed to fetch user data. Please try logging in again.');
            setIsLoading(false);
          }
        } else {
          console.log('DashboardPage - No user or token found, redirecting to login');
          navigate('/login');
        }
      } catch (error) {
        console.error('DashboardPage - Error during user check:', error);
        setError('An error occurred. Please try logging in again.');
        setIsLoading(false);
      }
    };
    
    checkUserAndRedirect();
  }, [user, token, setToken, setUser, setIsAuthenticated, navigate]);

  if (error) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-gray-50">
        <div className="max-w-md w-full p-6 bg-white rounded-lg shadow-md">
          <h2 className="text-2xl font-semibold text-center mb-6">Dashboard Error</h2>
          <div className="text-center text-red-600 mb-4">{error}</div>
          <button 
            onClick={() => navigate('/login')}
            className="w-full bg-indigo-600 text-white py-2 px-4 rounded hover:bg-indigo-700"
          >
            Return to Login
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen flex items-center justify-center bg-gray-50">
      <div className="max-w-md w-full p-6 bg-white rounded-lg shadow-md">
        <h2 className="text-2xl font-semibold text-center mb-6">Welcome to User Dashboard</h2>
        <p className="text-center mb-4">Redirecting you to the appropriate dashboard...</p>
        
        {user ? (
          <div className="text-center">
            <p className="mb-2">
              <span className="font-medium">Name:</span> {user.firstName} {user.lastName}
            </p>
            <p className="mb-2">
              <span className="font-medium">Email:</span> {user.email}
            </p>
            <p className="mb-2">
              <span className="font-medium">Role:</span> {user.role}
            </p>
            <div className="mt-4">
              <div className="animate-pulse flex justify-center">
                <div className="h-2 w-24 bg-indigo-500 rounded"></div>
              </div>
            </div>
          </div>
        ) : (
          <div className="text-center">
            <p className="text-gray-500 mb-4">Loading user information...</p>
            <div className="animate-spin rounded-full h-10 w-10 border-t-2 border-b-2 border-indigo-500 mx-auto"></div>
          </div>
        )}
      </div>
    </div>
  );
};

export default DashboardPage; 