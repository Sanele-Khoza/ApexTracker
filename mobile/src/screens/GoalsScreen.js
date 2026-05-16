import React, { useCallback, useState } from 'react';
import { View, Text, Pressable, RefreshControl, Alert } from 'react-native';
import { useFocusEffect } from '@react-navigation/native';
import tw from 'twrnc';
import { Screen, Card, Row, Btn, Empty, Field, Modal, ErrorBox, ProgressBar } from '../components/ui';
import { Api, todayStr, minutesToLabel } from '../api/client';

export default function GoalsScreen() {
  const [goals, setGoals] = useState([]);
  const [modal, setModal] = useState(false);
  const [form, setForm] = useState({ name: '', description: '', type: 'STUDY', duration: 7, value: '', reminderEnabled: false, reminderMinutes: 30 });
  const [error, setError] = useState('');
  const [refreshing, setRefreshing] = useState(false);

  const load = useCallback(async () => {
    try {
      setGoals(await Api.goals());
    } catch (e) {
      Alert.alert('Error', e.message);
    }
  }, []);

  useFocusEffect(useCallback(() => { load(); }, [load]));

  const save = async () => {
    setError('');
    if (!form.name.trim()) return setError('Goal name is required');
    const body = { name: form.name, description: form.description, type: form.type, duration: Number(form.duration) || 7, reminderEnabled: form.reminderEnabled, reminderMinutes: form.reminderMinutes };
    if (form.value) body.value = form.value;
    try {
      await Api.createGoal(body);
      setModal(false);
      load();
    } catch (e) {
      setError(e.message);
    }
  };

  const byStatus = (s) => goals.filter((g) => g.status === s);

  return (
    <Screen refreshControl={<RefreshControl refreshing={refreshing} onRefresh={() => { setRefreshing(true); load().finally(() => setRefreshing(false)); }} />}>
      <Row className="justify-between mb-2">
        <Text style={tw`text-white font-extrabold text-2xl`}>Goals</Text>
        <Btn title="+ Goal" onPress={() => { setForm({ name: '', description: '', type: 'STUDY', duration: 7, value: '', reminderEnabled: false, reminderMinutes: 30 }); setError(''); setModal(true); }} />
      </Row>

      {goals.length === 0 && <Empty text="Set a goal to start measuring progress." />}

      {(() => {
        const active = byStatus('IN_PROGRESS');
        const upcoming = byStatus('NOT_STARTED');
        const completed = byStatus('COMPLETED');
        return (
          <>
            {active.map((g) => <GoalCard key={g.id} g={g} onRefresh={load} />)}
            {upcoming.length > 0 && <Text style={tw`text-slate-400 font-bold mt-3 mb-1`}>Upcoming</Text>}
            {upcoming.map((g) => <GoalCard key={g.id} g={g} onRefresh={load} />)}
            {completed.length > 0 && <Text style={tw`text-slate-400 font-bold mt-3 mb-1`}>Completed</Text>}
            {completed.map((g) => <GoalCard key={g.id} g={g} onRefresh={load} />)}
          </>
        );
      })()}

      <Modal visible={modal} onClose={() => setModal(false)} title="New goal">
        <ErrorBox message={error} />
        <Field label="Name" value={form.name} onChangeText={(v) => setForm({ ...form, name: v })} placeholder="e.g. Finish linear algebra" />
        <Field label="Description (optional)" value={form.description} onChangeText={(v) => setForm({ ...form, description: v })} />
        <Text style={tw`text-xs text-slate-400 mb-1 ml-1`}>Type</Text>
        <View style={tw`flex-row flex-wrap gap-2 mb-3`}>
          {['STUDY', 'HEALTH', 'PERSONAL', 'PROJECT', 'FITNESS', 'OTHER'].map((t) => (
            <Pressable key={t} onPress={() => setForm({ ...form, type: t })}
              style={tw`px-3 py-1.5 rounded-full border ${form.type === t ? 'bg-indigo-600 border-indigo-500' : 'bg-slate-800 border-slate-700'}`}>
              <Text style={tw`text-xs ${form.type === t ? 'text-white font-semibold' : 'text-slate-300'}`}>{t}</Text>
            </Pressable>
          ))}
        </View>
        <Row className="gap-2">
          <View style={tw`flex-1`}><Field label="Duration (days)" keyboardType="numeric" value={String(form.duration)} onChangeText={(v) => setForm({ ...form, duration: Number(v) || 7 })} /></View>
          <View style={tw`flex-1`}><Field label="Target value (for goal type)" keyboardType="numeric" value={form.value} onChangeText={(v) => setForm({ ...form, value: v })} /></View>
        </Row>
        <Btn title="Save" onPress={save} />
      </Modal>
    </Screen>
  );
}

function GoalCard({ g, onRefresh }) {
  const pct = g.progress !== undefined && g.progress !== null ? Math.min(100, Math.max(0, Math.round(g.progress))) : 0;
  const color = g.status === 'COMPLETED' ? 'bg-emerald-600' : g.status === 'IN_PROGRESS' ? 'bg-violet-500' : 'bg-slate-600';
  const barColor = g.status === 'COMPLETED' ? '#34d399' : '#a78bfa';
  return (
    <Card className={`mb-3 ${g.status === 'COMPLETED' ? 'border-emerald-800' : ''} ${g.status === 'EXPIRED' ? 'border-rose-900' : ''}`}>
      <Row className="justify-between items-start mb-2">
        <View style={tw`flex-1 mr-2`}>
          <Row className="gap-2 mb-1">
            <Text style={tw`${color} text-white text-[10px] font-semibold px-2 py-0.5 rounded-full overflow-hidden`}>{g.type} · {g.status.replace('_', ' ')}</Text>
          </Row>
          <Text style={tw`text-white font-semibold text-base`}>{g.name}</Text>
          {g.description ? <Text style={tw`text-slate-400 text-xs`}>{g.description}</Text> : null}
        </View>
        <Pressable onPress={() => Alert.alert('Delete goal', 'Delete ' + g.name + '?', [
          { text: 'Cancel', style: 'cancel' },
          { text: 'Delete', style: 'destructive', onPress: async () => { await Api.deleteGoal(g.id); onRefresh(); } },
        ])}><Text style={tw`text-rose-400 text-xs`}>Delete</Text></Pressable>
      </Row>
      <Row className="justify-between mb-1">
        <Text style={tw`text-slate-400 text-xs`}>Progress {pct}%</Text>
        <Text style={tw`text-slate-400 text-xs`}>Day {g.daysElapsed ?? 0}/{g.duration ?? '—'}</Text>
      </Row>
      <ProgressBar pct={pct} color={barColor} />
      {g.expiresAt ? <Text style={tw`text-slate-500 text-[11px] mt-1`}>Until {g.expiresAt.slice(0, 10)}</Text> : null}
    </Card>
  );
}