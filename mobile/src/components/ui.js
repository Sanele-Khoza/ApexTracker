import React from 'react';
import { View, Text, TextInput, Pressable, ScrollView, ActivityIndicator, StyleSheet, KeyboardAvoidingView, Platform } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import tw from 'twrnc';

export function Screen({ children, scroll = true, refreshControl }) {
  const insets = useSafeAreaInsets();
  const Container = scroll ? ScrollView : View;
  return (
    <View style={[tw`flex-1 bg-slate-950`, { paddingTop: insets.top + 8 }]}>
      <Container style={tw`flex-1`} contentContainerStyle={scroll ? tw`p-4 pb-24` : undefined} refreshControl={refreshControl}>
        {children}
      </Container>
    </View>
  );
}

export function Card({ children, className = '' }) {
  return <View style={tw`bg-slate-900 rounded-2xl p-4 border border-slate-800 ${className}`}>{children}</View>;
}

export function Row({ children, className = '' }) {
  return <View style={tw`flex-row items-center ${className}`}>{children}</View>;
}

export function Btn({ title, onPress, variant = 'primary', disabled, loading, className = '' }) {
  const styles = {
    primary: tw`bg-indigo-600`,
    success: tw`bg-emerald-600`,
    danger: tw`bg-rose-600`,
    ghost: tw`bg-slate-800`,
  };
  return (
    <Pressable
      onPress={onPress}
      disabled={disabled || loading}
      style={({ pressed }) => [
        styles[variant],
        tw`py-3 px-4 rounded-xl items-center justify-center ${pressed ? 'opacity-80' : ''}`,
        disabled ? tw`opacity-50` : null,
        className,
      ]}
    >
      {loading ? <ActivityIndicator color="#fff" /> : <Text style={tw`text-white font-semibold`}>{title}</Text>}
    </Pressable>
  );
}

export function Stat({ label, value, sub, accent = '#818cf8' }) {
  return (
    <Card className="flex-1 min-w-[100px]">
      <Text style={tw`text-[11px] text-slate-400 uppercase tracking-wide`}>{label}</Text>
      <Text style={tw`text-2xl font-bold text-white mt-1`}>{value}</Text>
      {sub ? <Text style={tw`text-xs text-slate-500 mt-0.5`}>{sub}</Text> : null}
    </Card>
  );
}

export function Field({ label, ...props }) {
  return (
    <View style={tw`mb-3`}>
      {label ? <Text style={tw`text-xs font-medium text-slate-400 mb-1 ml-1`}>{label}</Text> : null}
      <TextInput
        placeholderTextColor="#475569"
        style={tw`bg-slate-900 border border-slate-700 rounded-xl px-3 py-2.5 text-white text-sm`}
        {...props}
      />
    </View>
  );
}

export function Badge({ children, color = 'bg-slate-700' }) {
  return <Text style={tw`${color} text-white text-xs font-medium px-2 py-0.5 rounded-full overflow-hidden`}>{children}</Text>;
}

export function Empty({ text }) {
  return <Text style={tw`text-center text-slate-500 mt-8`}>{text}</Text>;
}

export function ErrorBox({ message }) {
  if (!message) return null;
  return <Text style={tw`text-rose-400 text-sm mb-3`}>{message}</Text>;
}

export function Picker({ options, value, onChange, placeholder }) {
  // Simple horizontal option selector
  return (
    <View style={tw`flex-row flex-wrap gap-2 mb-3`}>
      {options.map((opt) => {
        const active = opt === value;
        return (
          <Pressable
            key={opt}
            onPress={() => onChange(opt)}
            style={tw`px-3 py-1.5 rounded-full border ${active ? 'bg-indigo-600 border-indigo-500' : 'bg-slate-800 border-slate-700'}`}
          >
            <Text style={tw`text-xs ${active ? 'text-white font-semibold' : 'text-slate-300'}`}>{opt}</Text>
          </Pressable>
        );
      })}
    </View>
  );
}

export function ProgressBar({ pct, color = '#818cf8' }) {
  const w = Math.max(0, Math.min(100, pct || 0));
  return (
    <View style={tw`h-2 rounded-full bg-slate-800 overflow-hidden`}>
      <View style={[tw`h-full rounded-full`, { width: `${w}%`, backgroundColor: color }]} />
    </View>
  );
}

export function SectionTitle({ children }) {
  return <Text style={tw`text-white font-bold text-lg mb-2`}>{children}</Text>;
}

export function Modal({ visible, onClose, children, title }) {
  if (!visible) return null;
  return (
    <View style={tw`absolute inset-0 bg-black/70 z-50 justify-center p-4`}>
      <Pressable style={tw`absolute inset-0`} onPress={onClose} />
      <KeyboardAvoidingView behavior={Platform.OS === 'ios' ? 'padding' : undefined}>
        <View style={tw`bg-slate-900 border border-slate-700 rounded-2xl p-5 max-h-[85%]`}>
          {title ? <Text style={tw`text-white font-bold text-lg mb-3`}>{title}</Text> : null}
          <ScrollView keyboardShouldPersistTaps="handled">{children}</ScrollView>
        </View>
      </KeyboardAvoidingView>
    </View>
  );
}