import React, { useCallback, useState } from 'react';
import { View, Text, Pressable, RefreshControl, Alert } from 'react-native';
import { useFocusEffect } from '@react-navigation/native';
import tw from 'twrnc';
import { Screen, Card, Row, Btn, Badge, Empty, Field, Picker, Modal, SectionTitle, ErrorBox } from '../components/ui';
import { Api, todayStr } from '../api/client';

const CATEGORIES = ['STUDY', 'PERSONAL', 'UNIVERSITY', 'WORK', 'EXERCISE', 'HOUSEHOLD', 'PROJECT', 'OTHER'];
const PRIORITY = ['LOW', 'MEDIUM', 'HIGH'];
const RECURRENCE = ['NONE', 'DAILY', 'WEEKLY', 'MONTHLY'];
const STATUSES = ['ALL', 'NOT_STARTED', 'IN_PROGRESS', 'COMPLETED', 'OVERDUE', 'SKIPPED'];
const statusColor = { COMPLETED: 'bg-emerald-600', OVERDUE: 'bg-rose-600', IN_PROGRESS: 'bg-sky-600', NOT_STARTED: 'bg-slate-600', SKIPPED: 'bg-slate-700' };
const prioColor = { HIGH: 'text-rose-400', MEDIUM: 'text-amber-400', LOW: 'text-slate-400' };

const emptyTask = { id: null, name: '', description: '', category: 'STUDY', priority: 'MEDIUM', dueDate: todayStr(), dueTime: '18:00', estimatedMinutes: 60, recurrence: 'NONE', reminderMinutes: 15 };

export default function TasksScreen() {
  const [tasks, setTasks] = useState([]);
  const [status, setStatus] = useState('ALL');
  const [modal, setModal] = useState(null);
  const [form, setForm] = useState(emptyTask);
  const [error, setError] = useState('');
  const [saving, setSaving] = useState(false);
  const [reschedule, setReschedule] = useState(null);
  const [refreshing, setRefreshing] = useState(false);

  const load = useCallback(async () => {
    try {
      const data = await Api.tasks(status === 'ALL' ? {} : { status });
      setTasks(data);
    } catch (e) {
      Alert.alert('Error', e.message);
    }
  }, [status]);

  useFocusEffect(useCallback(() => { load(); }, [load]));

  const save = async () => {
    setError('');
    if (!form.name.trim()) return setError('Task name is required');
    setSaving(true);
    try {
      if (form.id) await Api.updateTask(form.id, form);
      else await Api.createTask(form);
      setModal(null);
      load();
    } catch (e) {
      setError(e.message);
    } finally {
      setSaving(false);
    }
  };

  const act = async (fn, after) => {
    try { await fn(); load(); if (after) after(); } catch (e) { Alert.alert('Error', e.message); }
  };

  const doReschedule = async () => {
    try {
      await Api.rescheduleTask(reschedule.id, reschedule.date, reschedule.time);
      setReschedule(null);
      load();
    } catch (e) {
      Alert.alert('Error', e.message);
    }
  };

  return (
    <Screen refreshControl={<RefreshControl refreshing={refreshing} onRefresh={() => { setRefreshing(true); load().finally(() => setRefreshing(false)); }} />}>
      <Row className="justify-between mb-2">
        <Text style={tw`text-white font-extrabold text-2xl`}>Tasks</Text>
        <Btn title="+ New task" onPress={() => { setForm(emptyTask); setError(''); setModal(true); }} />
      </Row>
      <View style={tw`mb-2`}><Picker options={STATUSES} value={status} onChange={setStatus} /></View>

      {tasks.length === 0 && <Empty text="No tasks here yet." />}

      {tasks.map((task) => (
        <Card key={task.id} className={`mb-3 ${task.status === 'OVERDUE' ? 'border-rose-800' : ''}`}>
          <Row className="justify-between items-start">
            <View style={tw`flex-1 mr-2`}>
              <Row className="flex-wrap gap-x-2 gap-y-1 mb-1">
                <Badge color={statusColor[task.status] || 'bg-slate-600'}>{task.status.replace('_', ' ')}</Badge>
                <Text style={tw`${prioColor[task.priority]} text-xs font-bold`}>{task.priority}</Text>
                <Badge color="bg-indigo-700">{task.category}</Badge>
              </Row>
              <Text style={tw`text-white font-semibold text-base`}>{task.name}</Text>
              {task.description ? <Text style={tw`text-slate-400 text-xs mt-0.5`}>{task.description}</Text> : null}
              <Text style={tw`text-slate-500 text-xs mt-1`}>
                Due {task.dueDate || '—'} {task.dueTime || ''} · est {task.estimatedMinutes || 0}m{task.recurrence !== 'NONE' ? ` · repeats ${task.recurrence.toLowerCase()}` : ''}
              </Text>
            </View>
          </Row>

          <Row className="flex-wrap gap-2 mt-3">
            {(task.status === 'NOT_STARTED' || task.status === 'OVERDUE' || task.status === 'IN_PROGRESS') && (
              <Btn title="✓ Complete" variant="success" className="px-3 py-2 flex-1" onPress={() => act(() => Api.completeTask(task.id))} />
            )}
            {task.status === 'NOT_STARTED' && (
              <Btn title="Start" variant="ghost" className="px-3 py-2" onPress={() => act(() => Api.startTask(task.id))} />
            )}
            {(task.status === 'OVERDUE' || task.status === 'NOT_STARTED') && (
              <Btn title="Reschedule" variant="ghost" className="px-3 py-2" onPress={() => setReschedule({ id: task.id, date: task.dueDate || todayStr(), time: task.dueTime || '18:00' })} />
            )}
            {task.status === 'OVERDUE' && (
              <Btn title="Skip" variant="danger" className="px-3 py-2" onPress={() => act(() => Api.skipTask(task.id))} />
            )}
            <View style={tw`flex-row gap-2 mt-1`}>
              <Pressable onPress={() => { setForm({ ...task, dueDate: task.dueDate, dueTime: task.dueTime }); setError(''); setModal(true); }}><Text style={tw`text-indigo-400 text-xs`}>Edit</Text></Pressable>
              <Pressable onPress={() => act(() => Api.deleteTask(task.id))}><Text style={tw`text-rose-400 text-xs`}>Delete</Text></Pressable>
            </View>
          </Row>
        </Card>
      ))}

      <Modal visible={!!modal} onClose={() => setModal(null)} title={form.id ? 'Edit task' : 'New task'}>
        <ErrorBox message={error} />
        <Field label="Name" value={form.name} onChangeText={(v) => setForm({ ...form, name: v })} placeholder="e.g. Study Mathematics" />
        <Field label="Description" value={form.description} onChangeText={(v) => setForm({ ...form, description: v })} placeholder="Optional notes" />
        <Text style={tw`text-xs text-slate-400 mb-1 ml-1`}>Category</Text>
        <Picker options={CATEGORIES} value={form.category} onChange={(v) => setForm({ ...form, category: v })} />
        <Text style={tw`text-xs text-slate-400 mb-1 ml-1`}>Priority</Text>
        <Picker options={PRIORITY} value={form.priority} onChange={(v) => setForm({ ...form, priority: v })} />
        <Row className="gap-2">
          <View style={tw`flex-1`}><Field label="Due date" value={form.dueDate} onChangeText={(v) => setForm({ ...form, dueDate: v })} placeholder="YYYY-MM-DD" /></View>
          <View style={tw`w-24`}><Field label="Time" value={form.dueTime} onChangeText={(v) => setForm({ ...form, dueTime: v })} placeholder="HH:MM" /></View>
        </Row>
        <Row className="gap-2">
          <View style={tw`flex-1`}><Field label="Est. minutes" keyboardType="numeric" value={String(form.estimatedMinutes)} onChangeText={(v) => setForm({ ...form, estimatedMinutes: Number(v) || 0 })} /></View>
          <View style={tw`flex-1`}><Field label="Reminder (min before)" keyboardType="numeric" value={String(form.reminderMinutes)} onChangeText={(v) => setForm({ ...form, reminderMinutes: Number(v) || 0 })} /></View>
        </Row>
        <Text style={tw`text-xs text-slate-400 mb-1 ml-1`}>Recurrence</Text>
        <Picker options={RECURRENCE} value={form.recurrence} onChange={(v) => setForm({ ...form, recurrence: v })} />
        <Btn title="Save" onPress={save} loading={saving} />
      </Modal>

      <Modal visible={!!reschedule} onClose={() => setReschedule(null)} title="Reschedule task">
        {reschedule && (
          <>
            <Field label="New date" value={reschedule.date} onChangeText={(v) => setReschedule({ ...reschedule, date: v })} />
            <Field label="New time" value={reschedule.time} onChangeText={(v) => setReschedule({ ...reschedule, time: v })} />
            <Btn title="Reschedule" onPress={doReschedule} />
          </>
        )}
      </Modal>
    </Screen>
  );
}