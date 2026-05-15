import React, { createContext, useContext, useEffect, useState, useCallback } from 'react';
import AsyncStorage from '@react-native-async-storage/async-storage';
import { Api } from '../api/client';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [token, setToken] = useState(null);
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    (async () => {
      try {
        const t = await AsyncStorage.getItem('apex_token');
        const u = await AsyncStorage.getItem('apex_user');
        if (t) {
          setToken(t);
          setUser(u ? JSON.parse(u) : null);
        }
      } catch (e) {
        // ignore
      } finally {
        setLoading(false);
      }
    })();
  }, []);

  const login = useCallback(async (email, password) => {
    const res = await Api.login(email, password);
    await AsyncStorage.setItem('apex_token', res.token);
    await AsyncStorage.setItem('apex_user', JSON.stringify({ id: res.userId, name: res.name, email: res.email }));
    setToken(res.token);
    setUser({ id: res.userId, name: res.name, email: res.email });
  }, []);

  const register = useCallback(async (name, email, password) => {
    const res = await Api.register(name, email, password);
    await AsyncStorage.setItem('apex_token', res.token);
    await AsyncStorage.setItem('apex_user', JSON.stringify({ id: res.userId, name: res.name, email: res.email }));
    setToken(res.token);
    setUser({ id: res.userId, name: res.name, email: res.email });
  }, []);

  const logout = useCallback(async () => {
    await AsyncStorage.multiRemove(['apex_token', 'apex_user']);
    setToken(null);
    setUser(null);
  }, []);

  return (
    <AuthContext.Provider value={{ token, user, loading, login, register, logout }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  return useContext(AuthContext);
}