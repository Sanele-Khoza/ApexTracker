import React from 'react';
import { NavigationContainer, DarkTheme } from '@react-navigation/native';
import { createNativeStackNavigator } from '@react-navigation/native-stack';
import { createBottomTabNavigator } from '@react-navigation/bottom-tabs';
import { SafeAreaProvider } from 'react-native-safe-area-context';
import { Ionicons } from '@expo/vector-icons';

import { AuthProvider, useAuth } from './src/context/AuthContext';
import AuthScreen from './src/screens/AuthScreen';
import DashboardScreen from './src/screens/DashboardScreen';
import TasksScreen from './src/screens/TasksScreen';
import ActivitiesScreen from './src/screens/ActivitiesScreen';
import StudyScreen from './src/screens/StudyScreen';
import SleepScreen from './src/screens/SleepScreen';
import HabitsScreen from './src/screens/HabitsScreen';
import GoalsScreen from './src/screens/GoalsScreen';
import CalendarScreen from './src/screens/CalendarScreen';
import AnalyticsScreen from './src/screens/AnalyticsScreen';
import InsightsScreen from './src/screens/InsightsScreen';
import DailyReviewScreen from './src/screens/DailyReviewScreen';
import PlanActualScreen from './src/screens/PlanActualScreen';
import SettingsScreen from './src/screens/SettingsScreen';

const AuthStack = createNativeStackNavigator();
const TrackStack = createNativeStackNavigator();
const ProgressStack = createNativeStackNavigator();
const Tab = createBottomTabNavigator();

const darkHeader = {
  headerStyle: { backgroundColor: '#0f172a' },
  headerTintColor: '#fff',
  headerTitleStyle: { color: '#fff', fontWeight: 'bold' },
  contentStyle: { backgroundColor: '#020617' },
};

function TrackNavigator() {
  return (
    <TrackStack.Navigator screenOptions={darkHeader}>
      <TrackStack.Screen name="Activities" component={ActivitiesScreen} />
      <TrackStack.Screen name="Study" component={StudyScreen} />
      <TrackStack.Screen name="Sleep" component={SleepScreen} />
      <TrackStack.Screen name="Habits" component={HabitsScreen} />
    </TrackStack.Navigator>
  );
}

function ProgressNavigator() {
  return (
    <ProgressStack.Navigator screenOptions={darkHeader}>
      <ProgressStack.Screen name="Goals" component={GoalsScreen} />
      <ProgressStack.Screen name="Calendar" component={CalendarScreen} />
      <ProgressStack.Screen name="Analytics" component={AnalyticsScreen} />
      <ProgressStack.Screen name="Insights" component={InsightsScreen} />
      <ProgressStack.Screen name="DailyReview" component={DailyReviewScreen} />
      <ProgressStack.Screen name="PlanActual" component={PlanActualScreen} />
    </ProgressStack.Navigator>
  );
}

function MainTabs() {
  return (
    <Tab.Navigator
      screenOptions={({ route }) => ({
        headerShown: false,
        tabBarStyle: { backgroundColor: '#0f172a', borderTopColor: '#1e293b' },
        tabBarActiveTintColor: '#818cf8',
        tabBarInactiveTintColor: '#64748b',
        tabBarIcon: ({ color, size }) => {
          const iconMap = {
            Home: 'home',
            Tasks: 'checkmark-circle',
            Track: 'timer',
            Progress: 'stats-chart',
            Settings: 'settings',
          };
          return <Ionicons name={iconMap[route.name]} size={size} color={color} />;
        },
      })}
    >
      <Tab.Screen name="Home" component={DashboardScreen} />
      <Tab.Screen name="Tasks" component={TasksScreen} />
      <Tab.Screen name="Track" component={TrackNavigator} />
      <Tab.Screen name="Progress" component={ProgressNavigator} />
      <Tab.Screen name="Settings" component={SettingsScreen} />
    </Tab.Navigator>
  );
}

function Root() {
  const { token, loading } = useAuth();
  if (loading) return null;
  return (
    <NavigationContainer theme={{ ...DarkTheme, colors: { ...DarkTheme.colors, background: '#020617', card: '#0f172a', text: '#fff', primary: '#818cf8' } }}>
      {token ? (
        <MainTabs />
      ) : (
        <AuthStack.Navigator screenOptions={{ headerShown: false }}>
          <AuthStack.Screen name="Auth" component={AuthScreen} />
        </AuthStack.Navigator>
      )}
    </NavigationContainer>
  );
}

export default function App() {
  return (
    <SafeAreaProvider>
      <AuthProvider>
        <Root />
      </AuthProvider>
    </SafeAreaProvider>
  );
}