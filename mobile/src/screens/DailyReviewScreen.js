import React, { useCallback, useState } from 'react';
import { View, Text, RefreshControl, Alert } from 'react-native';
import { useFocusEffect } from '@react-navigation/native';
import tw from 'twrnc';
import { Screen, Card, Row, Btn, Field, ErrorBox, Empty } from '../components/ui';
import { Api, todayStr } from '../api/client';

export default function DailyReviewScreen() {
  const [date, setDate] = useState(todayStr());
  const [review, setReview] = useState(null);
  const [reflection, setReflection] = useState('');
  const [error, setError] = useState('');
  const [refreshing, setRefreshing] = useState(false);

  const load = useCallback(async () => {
    try {
      const d = await Api.review(date);
      setReview(d);
      setReflection(d.reflection || '');
    } catch (e) {
      setReview(null);
      setReflection('');
    }
  }, [date]);

  useFocusEffect(useCallback(() => { load(); }, [load]));

  const saveReflection = async () => {
    try {
      await Api.updateReviewReflection(date, reflection);
      Alert.alert('Saved', 'Reflection updated.');
      load();
    } catch (e) {
      setError(e.message);
    }
  };

  return (
    <Screen refreshControl={<RefreshControl refreshing={refreshing} onRefresh={() => { setRefreshing(true); load().finally(() => setRefreshing(false)); }} />}>
      <Text style={tw`text-white font-extrabold text-2xl mb-2`}>Daily review</Text>
      <Field label="Date" value={date} onChangeText={setDate} placeholder="YYYY-MM-DD" />
      <Btn title="Load review" variant="ghost" onPress={load} className="mb-4" />

      {review ? (
        <>
          <Card className="mb-3">
            <Row className="justify-between mb-1"><Text style={tw`text-slate-400 text-xs`}>Performance</Text><Text style={tw`text-white font-bold`}>{review.performanceScore ?? '—'}/100</Text></Row>
            <Row className="justify-between mb-1"><Text style={tw`text-slate-400 text-xs`}>Tasks</Text><Text style={tw`text-slate-200 text-sm`}>{review.tasksCompleted}/{review.tasksPlanned} done</Text></Row>
            <Row className="justify-between mb-1"><Text style={tw`text-slate-400 text-xs`}>Study</Text><Text style={tw`text-slate-200 text-sm`}>{review.studyMinutes ?? 0} min</Text></Row>
            <Row className="justify-between mb-1"><Text style={tw`text-slate-400 text-xs`}>Sleep</Text><Text style={tw`text-slate-200 text-sm`}>{review.sleepMinutes ?? 0} min</Text></Row>
            <Row className="justify-between"><Text style={tw`text-slate-400 text-xs`}>Habits</Text><Text style={tw`text-slate-200 text-sm`}>{review.habitsCompleted}/{review.habitsTotal}</Text></Row>
          </Card>

          {review.headline ? (
            <Card className="mb-3 border-indigo-800">
              <Text style={tw`text-indigo-300 text-xs font-semibold uppercase mb-1`}>Summary</Text>
              <Text style={tw`text-slate-200`}>{review.headline}</Text>
            </Card>
          ) : null}

          {review.todaysAccomplishments && review.todaysAccomplishments.length > 0 && (
            <Card className="mb-3">
              <Text style={tw`text-emerald-300 text-xs font-semibold uppercase mb-1`}>Accomplishments</Text>
              {review.todaysAccomplishments.map((a, i) => <Text key={i} style={tw`text-slate-300 text-sm mb-1`}>• {a}</Text>)}
            </Card>
          )}
          {review.todaysChallenges && review.todaysChallenges.length > 0 && (
            <Card className="mb-3">
              <Text style={tw`text-rose-300 text-xs font-semibold uppercase mb-1`}>Challenges</Text>
              {review.todaysChallenges.map((a, i) => <Text key={i} style={tw`text-slate-300 text-sm mb-1`}>• {a}</Text>)}
            </Card>
          )}
          {review.tomorrowFocus && review.tomorrowFocus.length > 0 && (
            <Card className="mb-3">
              <Text style={tw`text-sky-300 text-xs font-semibold uppercase mb-1`}>Tomorrow's focus</Text>
              {review.tomorrowFocus.map((a, i) => <Text key={i} style={tw`text-slate-300 text-sm mb-1`}>• {a}</Text>)}
            </Card>
          )}

          <Text style={tw`text-white font-bold mb-2 mt-3`}>Your reflection</Text>
          <Field
            value={reflection}
            onChangeText={setReflection}
            placeholder="How did today go? What will you change?"
            multiline
            numberOfLines={4}
            style={tw`min-h-[90px] text-white`}
          />
          <ErrorBox message={error} />
          <Btn title="Save reflection" onPress={saveReflection} />
        </>
      ) : (
        <Empty text="No review generated for this date yet. Reviews are created automatically at midnight but you can come back and enter one later." />
      )}
    </Screen>
  );
}