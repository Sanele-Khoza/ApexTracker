import React, { useState } from 'react';
import { View, Text, KeyboardAvoidingView, Platform } from 'react-native';
import tw from 'twrnc';
import { Field, Btn, ErrorBox } from '../components/ui';
import { useAuth } from '../context/AuthContext';
import { ApiError } from '../api/client';

export default function LoginScreen({ navigation }) {
  const { login, register } = useAuth();
  const [mode, setMode] = useState('login');
  const [name, setName] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const submit = async () => {
    setError('');
    if (!email.trim() || password.length === 0) return setError('Email and password are required');
    if (mode === 'register' && name.trim().length === 0) return setError('Name is required');
    if (mode === 'register' && password.length < 6) return setError('Password must be at least 6 characters');
    setLoading(true);
    try {
      if (mode === 'login') await login(email.trim(), password);
      else await register(name.trim(), email.trim(), password);
    } catch (e) {
      setError(e instanceof ApiError ? e.message : 'Something went wrong');
    } finally {
      setLoading(false);
    }
  };

  return (
    <KeyboardAvoidingView behavior={Platform.OS === 'ios' ? 'padding' : undefined} style={tw`flex-1 bg-slate-950 justify-center p-6`}>
      <Text style={tw`text-3xl font-extrabold text-white text-center mb-1`}>ApexTracker</Text>
      <Text style={tw`text-center text-slate-400 mb-8`}>Plan → Track → Compare → Analyze → Improve</Text>
      <View style={tw`flex-row gap-2 mb-5 justify-center`}>
        {['login', 'register'].map((m) => (
          <View key={m} style={tw`flex-1`}>
            <Btn
              title={m === 'login' ? 'Login' : 'Register'}
              variant={mode === m ? 'primary' : 'ghost'}
              onPress={() => { setMode(m); setError(''); }}
            />
          </View>
        ))}
      </View>
      <ErrorBox message={error} />
      {mode === 'register' && <Field label="Name" value={name} onChangeText={setName} placeholder="Your name" autoCapitalize="words" />}
      <Field label="Email" value={email} onChangeText={setEmail} placeholder="you@example.com" autoCapitalize="none" keyboardType="email-address" />
      <Field label="Password" value={password} onChangeText={setPassword} placeholder="••••••••" secureTextEntry />
      <Btn title={mode === 'login' ? 'Log in' : 'Create account'} onPress={submit} loading={loading} />
    </KeyboardAvoidingView>
  );
}