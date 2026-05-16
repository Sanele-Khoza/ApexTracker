import React, { useCallback, useState, useEffect } from 'react';
import { View, Text, Pressable, RefreshControl, Alert } from 'react-native';
import { useFocusEffect } from '@react-navigation/native';
import tw from 'twrnc';
import { Screen, Card, Row, Btn, Empty, Field, Modal, ErrorBox, ProgressBar } from '../components/ui';
import { Api, minutesToLabel } from '../api/client';

export default function StudyScreen() {
  const [data, setData] = useState(null);
  const [modal, setModal] = useState(false);
  const [name, setName] = useState('');
  const [subject, setSubject] = useState('');
  const [goalMinutes, setGoalMinutes] = useState('');
  const [seconds, setSeconds] = useState(0);
  const [refreshing, setRefreshing] = useState(false);

  const load = useCallback(async () => {
    try {
      const d = await Api.studySessions();
      setData(d);
      if (d.activeSession) setSeconds(Math.round((Date.now() - new Date(d.activeSession.startTime).getTime()) / 1000));
    } catch (e) {
      Alert.alert('Error', e.message);
    }
  }, []);

  useFocusEffect(useCallback(() => { load(); }, [load]));

  useEffect(() => {
    const id = setInterval(() => setSeconds((s) => s + 1), 1000);
    return () => clearInterval(id);
  }, []);

  const start = async () => {
    try {
      await Api.startStudy({ name, subject, goalMinutes: Number(goalMinutes) || 0 });
      setModal(false);
      setName(''); setSubject(''); setGoalMinutes('');
      load();
    } catch (e) {
      Alert.alert('Error', e.message);
    }
  };

  const fmt = (sec) => {
    const h = Math.floor(sec / 3600);
    const m = Math.floor((sec % 3600) / 60);
    const s = sec % 60;
    return `${String(h).padStart(2, '0')}:${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`;
  };

  if (!data) return <Screen scroll={false}><Text style={tw`text-slate-400 text-center mt-10`}>Loading…</Text></Screen>;

  const active = data.activeSession;

  return (
    <Screen refreshControl={<RefreshControl refreshing={refreshing} onRefresh={() => { setRefreshing(true); load().finally(() => setRefreshing(false)); }} />}>
      <Row className="justify-between mb-2">
        <Text style={tw`text-white font-extrabold text-2xl`}>Study</Text>
        <Btn title="+ Session" onPress={() => setModal(true)} />
      </Row>

      <Card className={`mb-4 ${active ? 'border-sky-600' : ''}`}>
        {active ? (
          <>
            <Text style={tw`text-sky-400 text-xs font-semibold uppercase`}>&bull; Studying{active.pauses && active.pauses.length ? ' (paused)' : ''}</Text>
            <Text style={tw`text-white text-lg font-bold mt-1`}>{active.name}</Text>
            <Text style={tw`text-slate-400 text-xs`}>{active.subject} · started {new Date(active.startTime).toLocaleTimeString()}</Text>
            <Text style={tw`text-white text-3xl font-bold mt-3 text-center`}>{fmt(seconds)}</Text>
            <Row className="gap-2 mt-3">
              <View style={tw`flex-1`}>
                {active.pauses && active.pauses.length % 2 === 1 ? (
                  <Btn title="Resume" variant="ghost" onPress={async () => { await Api.resumeStudy(active.id); load(); }} />
                ) : (
                  <Btn title="Pause" variant="ghost" onPress={async () => { await Api.pauseStudy(active.id); load(); }} />
                )}
              </View>
              <View style={tw`flex-1`}>
                <Btn title="Finish" variant="success" onPress={async () => { await Api.finishStudy(active.id); load(); }} />
              </View>
            </Row>
          </>
        ) : (
          <>
            <Text style={tw`text-slate-400`}>No active study session.</Text>
            <Text style={tw`text-xs text-slate-500 mt-1`}>Start a focused session to build your study stats.</Text>
          </>
        )}
      </Card>

      <Card className="mb-4">
        <Text style={tw`text-xs text-slate-400 uppercase`}>Weekly progress</Text>
        <Row className="justify-between mt-1 mb-2">
          <Text style={tw`text-white font-bold`}>{minutesToLabel(data.weekMinutes)}</Text>
          <Text style={tw`text-slate-400 text-sm`}>goal {minutesToLabel(data.weeklyGoal)}</Text>
        </Row>
        <ProgressBar pct={(data.weekMinutes / Math.max(1, data.weeklyGoal)) * 100} color="#38bdf8" />
      </Card>

      {data.sessions && data.sessions.length === 0 && <Empty text="No study sessions yet." />}
      {data.sessions && data.sessions.map((s) => (
        <Card key={s.id} className="mb-2 py-3">
          <Row className="justify-between">
            <View style={tw`flex-1`}>
              <Row className="gap-2 mb-1">
                <Text style={tw`text-white font-semibold`}>{s.name}</Text>
                {s.subject ? <Badge2>{s.subject}</Badge2> : null}
              </Row>
              <Text style={tw`text-xs text-slate-500`}>
                {s.date} · {s.startTime} → {s.endTime || 'ongoing'} · {minutesToLabel(s.durationMinutes)}{s.totalPauseMinutes > 0 ? ` (${minutesToLabel(s.totalPauseMinutes)} paused)` : ''}
              </Text>
              {s.goalMinutes > 0 && <Text style={tw`text-xs text-slate-500`}>goal {s.goalMinutes}m{Number.isFinite(s.completionRatio) ? ` · ${Math.round(s.completionRatio * 100)}%` : ''}</Text>}
            </View>
            <Pressable onPress={() => Alert.alert('Delete session', 'Delete this study session?', [
              { text: 'Cancel', style: 'cancel' },
              { text: 'Delete', style: 'destructive', onPress: async () => { await Api.deleteStudy(s.id); load(); } },
            ])}><Text style={tw`text-rose-400 text-xs`}>Delete</Text></Pressable>
          </Row>
        </Card>
      ))}

      <Modal visible={modal} onClose={() => setModal(false)} title="New study session">
        <Field label="What are you studying?" value={name} onChangeText={setName} placeholder="e.g. Algebra chapter 4" />
        <Field label="Subject" value={subject} onChangeText={setSubject} placeholder="e.g. Mathematics" />
        <Field label="Session goal (minutes)" keyboardType="numeric" value={goalMinutes} onChangeText={setGoalMinutes} placeholder="e.g. 60" />
        <Btn title="Start session" onPress={start} />
      </Modal>
    </Screen>
  );
}

function Badge2({ children }) {
  return <Text style={tw`bg-slate-800 text-slate-300 text-xs px-2 py-0.5 rounded-full overflow-hidden`}>{children}</Text>;
}