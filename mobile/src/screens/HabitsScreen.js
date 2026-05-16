import React, { useCallback, useState } from 'react';
import { View, Text, Pressable, RefreshControl, Alert } from 'react-native';
import { useFocusEffect } from '@react-navigation/native';
import tw from 'twrnc';
import { Screen, Card, Row, Btn, Empty, Field, Modal, ErrorBox, ProgressBar } from '../components/ui';
import { Api, todayStr } from '../api/client';

const CATEGORIES = ['MORNING', 'EVENING', 'HEALTH', 'MENTAL', 'UNIVERSITY', 'PERSONAL', 'HOUSEHOLD', 'OTHER'];

export default function HabitsScreen() {
  const [data, setData] = useState(null);
  const [modal, setModal] = useState(false);
  const [form, setForm] = useState({ name: '', category: 'HEALTH', targetDays: 7, reminderEnabled: false, reminderMinutes: 15 });
  const [error, setError] = useState('');
  const [refreshing, setRefreshing] = useState(false);

  const load = useCallback(async () => {
    try {
      const d = await Api.habits();
      setData(d);
    } catch (e) {
      Alert.alert('Error', e.message);
    }
  }, []);

  useFocusEffect(useCallback(() => { load(); }, [load]));

  const save = async () => {
    setError('');
    if (!form.name.trim()) return setError('Habit name is required');
    if (form.targetDays < 1 || form.targetDays > 7) return setError('Target must be 1–7 days per week');
    try {
      await Api.createHabit(form);
      setModal(false);
      load();
    } catch (e) {
      setError(e.message);
    }
  };

  const toggle = async (h) => {
    try {
      await Api.toggleHabit(h.id, todayStr());
      load();
    } catch (e) {
      Alert.alert('Error', e.message);
    }
  };

  return (
    <Screen refreshControl={<RefreshControl refreshing={refreshing} onRefresh={() => { setRefreshing(true); load().finally(() => setRefreshing(false)); }} />}>
      <Row className="justify-between mb-2">
        <Text style={tw`text-white font-extrabold text-2xl`}>Habits</Text>
        <Btn title="+ Habit" onPress={() => { setForm({ name: '', category: 'HEALTH', targetDays: 7, reminderEnabled: false, reminderMinutes: 15 }); setError(''); setModal(true); }} />
      </Row>

      {data && (
        <>
          <Card className="mb-4">
            <Text style={tw`text-xs text-slate-400 uppercase`}>Today</Text>
            <Row className="justify-between mt-1 mb-2">
              <Text style={tw`text-white font-bold`}>{data.todayCompleted}/{data.totalHabits} habits done</Text>
            </Row>
            <ProgressBar pct={data.totalHabits ? (data.todayCompleted / data.totalHabits) * 100 : 0} color="#fbbf24" />
          </Card>

          {data.habits && data.habits.length === 0 && <Empty text="No habits yet. Build your routine!" />}
          {data.habits && data.habits.map((h) => {
            const doneToday = h.completedToday;
            return (
              <Card key={h.id} className={`mb-3 ${doneToday ? 'border-emerald-700' : ''}`}>
                <Row className="justify-between items-start">
                  <View style={tw`flex-1 mr-2`}>
                    <Row className="gap-2 mb-1">
                      <Text style={tw`bg-indigo-700 text-white text-[10px] font-semibold px-2 py-0.5 rounded-full overflow-hidden`}>{h.category}</Text>
                      <Text style={tw`text-slate-400 text-xs`}>target {h.targetDays}/week</Text>
                    </Row>
                    <Text style={tw`text-white font-semibold text-base`}>{h.name}</Text>
                    <Text style={tw`text-xs text-slate-500`}>
                      {h.totalCompletions} total · streak {h.currentStreak} day{h.currentStreak === 1 ? '' : 's'}
                      {h.bestStreak > h.currentStreak ? ` · best ${h.bestStreak}` : ''}
                    </Text>
                  </View>
                  <Pressable onPress={() => toggle(h)}
                    style={tw`w-11 h-11 rounded-xl items-center justify-center border ${doneToday ? 'bg-emerald-600 border-emerald-500' : 'bg-slate-800 border-slate-700 border-dashed'}`}>
                    <Text style={tw`${doneToday ? 'text-white' : 'text-slate-400'} text-xl font-bold`}>{doneToday ? '✓' : '+'}</Text>
                  </Pressable>
                </Row>
                <Row className="gap-3 mt-2">
                  <Pressable onPress={() => Alert.alert('Habit', 'This habit is checked off for today. Untoggle by tapping the box again.')}><Text style={tw`text-indigo-400 text-xs`}>Info</Text></Pressable>
                  <Pressable onPress={() => Alert.alert('Delete habit', 'Delete ' + h.name + '? This keeps past completions.', [
                    { text: 'Cancel', style: 'cancel' },
                    { text: 'Delete', style: 'destructive', onPress: async () => { await Api.deleteHabit(h.id); load(); } },
                  ])}><Text style={tw`text-rose-400 text-xs`}>Delete</Text></Pressable>
                </Row>
              </Card>
            );
          })}
        </>
      )}

      <Modal visible={modal} onClose={() => setModal(false)} title="New habit">
        <ErrorBox message={error} />
        <Field label="Name" value={form.name} onChangeText={(v) => setForm({ ...form, name: v })} placeholder="e.g. Morning run" />
        <Text style={tw`text-xs text-slate-400 mb-1 ml-1`}>Category</Text>
        <View style={tw`flex-row flex-wrap gap-2 mb-3`}>
          {CATEGORIES.map((c) => (
            <Pressable key={c} onPress={() => setForm({ ...form, category: c })}
              style={tw`px-3 py-1.5 rounded-full border ${form.category === c ? 'bg-indigo-600 border-indigo-500' : 'bg-slate-800 border-slate-700'}`}>
              <Text style={tw`text-xs ${form.category === c ? 'text-white font-semibold' : 'text-slate-300'}`}>{c}</Text>
            </Pressable>
          ))}
        </View>
        <Field label="Target days per week (1–7)" keyboardType="numeric" value={String(form.targetDays)} onChangeText={(v) => setForm({ ...form, targetDays: Number(v) || 7 })} />
        <Row className="justify-between mb-3">
          <Text style={tw`text-slate-300 text-sm`}>Daily reminder</Text>
          <Pressable onPress={() => setForm({ ...form, reminderEnabled: !form.reminderEnabled })}
            style={tw`w-12 h-7 rounded-full ${form.reminderEnabled ? 'bg-indigo-600' : 'bg-slate-700'} justify-center px-1`}>
            <View style={tw`w-5 h-5 rounded-full bg-white ${form.reminderEnabled ? 'self-end' : 'self-start'}`} />
          </Pressable>
        </Row>
        {form.reminderEnabled && <Field label="Remind me after (minutes after wake time)" keyboardType="numeric" value={String(form.reminderMinutes)} onChangeText={(v) => setForm({ ...form, reminderMinutes: Number(v) || 15 })} />}
        <Btn title="Save" onPress={save} />
      </Modal>
    </Screen>
  );
}