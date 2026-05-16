import React, { useCallback, useState } from 'react';
import { View, Text, Pressable, RefreshControl } from 'react-native';
import { useFocusEffect } from '@react-navigation/native';
import tw from 'twrnc';
import { Screen, Card, Stat, SectionTitle, Badge, Empty, ProgressBar, Row, Btn } from '../components/ui';
import { Api, minutesToLabel } from '../api/client';
import { useAuth } from '../context/AuthContext';

const statusColor = { COMPLETED: 'bg-emerald-600', OVERDUE: 'bg-rose-600', IN_PROGRESS: 'bg-sky-600', NOT_STARTED: 'bg-slate-600', SKIPPED: 'bg-slate-700' };

export default function DashboardScreen({ navigation }) {
  const { logout } = useAuth();
  const [data, setData] = useState(null);
  const [refreshing, setRefreshing] = useState(false);
  const [loading, setLoading] = useState(true);

  const load = useCallback(async () => {
    try {
      const d = await Api.dashboard();
      setData(d);
    } catch (e) {
      if (e.status === 401) logout();
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  }, [logout]);

  useFocusEffect(
    useCallback(() => {
      load();
    }, [load])
  );

  if (loading) return <Screen scroll={false}><Text style={tw`text-slate-400 text-center mt-10`}>Loading…</Text></Screen>;

  const t = data.tasks;

  return (
    <Screen refreshControl={<RefreshControl refreshing={refreshing} onRefresh={() => { setRefreshing(true); load(); }} />}>
      <Row className="justify-between mb-1">
        <Text style={tw`text-white font-extrabold text-2xl`}>Today</Text>
        <Text style={tw`text-slate-400`}>{data.date} · {data.time}</Text>
      </Row>
      <Text style={tw`text-slate-400 text-sm mb-4`}>Welcome back, {data.name}</Text>

      <View style={tw`flex-row flex-wrap gap-3`}>
        <Stat label="Tasks" value={t.planned} sub={`${t.completed} done · ${t.pending} left`} />
        <Stat label="Overdue" value={t.overdue} sub="needs attention" accent="#fb7185" />
        <Stat label="Studied" value={minutesToLabel(data.studyMinutes)} accent="#34d399" />
        <Stat label="Sleep" value={minutesToLabel(data.sleepMinutes)} sub={data.sleepRecorded ? 'last night' : 'not recorded'} accent="#38bdf8" />
        <Stat label="Habits" value={`${data.habitsCompleted}/${data.habitsTotal}`} accent="#fbbf24" />
        <Stat label="Productivity" value={`${data.productivityScore}%`} accent="#a78bfa" />
      </View>

      <SectionTitle>Performance</SectionTitle>
      <Card>
        <Row className="justify-between mb-1">
          <Text style={tw`text-white font-bold`}>Overall {data.performanceScore}/100</Text>
          <Text style={tw`text-[10px] text-slate-500`}>{data.scoreDisclaimer}</Text>
        </Row>
        <Text style={tw`text-xs text-slate-400 mb-2`}>P {data.scores.productivity} · S {data.scores.study} · R {data.scores.routine} · Slp {data.scores.sleep} · A {data.scores.activity}</Text>
        <ProgressBar pct={data.performanceScore} color="#a78bfa" />
      </Card>

      {data.overdueTasks.length > 0 && (
        <>
          <SectionTitle>⚠ Overdue tasks</SectionTitle>
          {data.overdueTasks.map((task) => (
            <Card key={task.id} className="mb-3 border-rose-800">
              <Row className="justify-between">
                <View style={tw`flex-1`}>
                  <Text style={tw`text-white font-semibold`}>{task.name}</Text>
                  <Text style={tw`text-xs text-slate-400`}>Due {task.dueDate} {task.dueTime || ''} · {task.category}</Text>
                </View>
                <Btn title="Complete" variant="success" onPress={async () => { await Api.completeTask(task.id); load(); }} className="px-3 py-2" />
              </Row>
            </Card>
          ))}
        </>
      )}

      {data.activeActivity && (
        <>
          <SectionTitle>⏱ Running now</SectionTitle>
          <Card className="border-emerald-700">
            <Text style={tw`text-white font-semibold text-lg`}>{data.activeActivity.name}</Text>
            <Row className="justify-between mt-2">
              <Text style={tw`text-xs text-slate-400`}>since {new Date(data.activeActivity.startTime).toLocaleTimeString()}</Text>
              <Btn title="Stop" variant="success" onPress={async () => { await Api.stopActivity(data.activeActivity.id); load(); }} className="px-3 py-2" />
            </Row>
          </Card>
        </>
      )}

      <SectionTitle>Upcoming reminders</SectionTitle>
      {data.upcoming.length === 0 && !data.reminders.length && <Empty text="Nothing scheduled — enjoy the quiet." />}
      {data.upcoming.map((u) => (
        <Card key={`u${u.id}`} className="mb-2 py-3"><Row><Badge color="bg-indigo-600">{u.category}</Badge><Text style={tw`text-white text-sm ml-2 flex-1`}>{u.name}</Text><Text style={tw`text-slate-400 text-xs`}>{u.dueTime}</Text></Row></Card>
      ))}
      {data.reminders.map((r) => (
        <Card key={`r${r.id}`} className="mb-2 py-3"><Row className="justify-between">
          <View style={tw`flex-1 mr-2`}><Text style={tw`text-sm text-slate-200`}>{r.message}</Text></View>
          <Pressable onPress={async () => { await Api.dismissReminder(r.id); load(); }}><Text style={tw`text-rose-400 text-xs font-semibold`}>Dismiss</Text></Pressable>
        </Row></Card>
      ))}

      <Btn title="Daily review" variant="ghost" onPress={() => navigation.navigate('Progress', { screen: 'DailyReview' })} className="mt-2" />
      <Btn title="Plan vs actual" variant="ghost" onPress={() => navigation.navigate('Progress', { screen: 'PlanActual' })} className="mt-2" />
    </Screen>
  );
}