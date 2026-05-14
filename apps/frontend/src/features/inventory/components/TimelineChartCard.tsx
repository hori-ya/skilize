import type { TimelineEvent, TimelineEventType } from '../types/charts';

interface Props {
  events: TimelineEvent[];
}

const EVENT_LABEL: Record<TimelineEventType, string> = {
  QUALIFICATION: '資格取得',
  AD_SEMINAR: 'ADセミナー',
  FREE_SEMINAR: '自由セミナー',
  GOAL_QUALIFICATION: '目標：資格',
  GOAL_IT_SKILL: '目標：ITスキル',
  GOAL_AD: '目標：AD',
};

const EVENT_ICON: Record<TimelineEventType, string> = {
  QUALIFICATION: '🏅',
  AD_SEMINAR: '📚',
  FREE_SEMINAR: '🎓',
  GOAL_QUALIFICATION: '🎯',
  GOAL_IT_SKILL: '🎯',
  GOAL_AD: '🎯',
};

function formatYearMonth(yearMonth: string): string {
  const [year, month] = yearMonth.split('-');
  return `${year}年${parseInt(month)}月`;
}

export default function TimelineChartCard({ events }: Props) {
  if (events.length === 0) {
    return (
      <div className="chart-card chart-card--full">
        <p className="chart-card__title">ラーニングタイムライン</p>
        <div className="chart-no-data">記録された実績・目標がありません</div>
      </div>
    );
  }

  const achievement = events.filter(e => e.lane === 'ACHIEVEMENT');
  const activity = events.filter(e => e.lane === 'ACTIVITY');

  return (
    <div className="chart-card chart-card--full">
      <p className="chart-card__title">ラーニングタイムライン</p>
      <div className="timeline">
        <TimelineLane label="資格取得実績" events={achievement} colorClass="timeline-lane--achievement" />
        <TimelineLane label="セミナー受講実績" events={activity} colorClass="timeline-lane--activity" />
      </div>
    </div>
  );
}

function TimelineLane({
  label,
  events,
  colorClass,
}: {
  label: string;
  events: TimelineEvent[];
  colorClass: string;
}) {
  return (
    <div className={`timeline-lane ${colorClass}`}>
      <div className="timeline-lane__header">{label}</div>
      {events.length === 0 ? (
        <div className="timeline-lane__empty">記録なし</div>
      ) : (
        <div className="timeline-lane__track">
          <div className="timeline-lane__line" />
          <div className="timeline-lane__events">
            {events.map((ev, i) => (
              <div key={i} className={`timeline-event${ev.isPast ? '' : ' timeline-event--goal'}`}>
                <div className="timeline-event__dot" />
                <div className="timeline-event__body">
                  <span className="timeline-event__icon">{EVENT_ICON[ev.type]}</span>
                  <span className="timeline-event__name">{ev.name}</span>
                  <span className="timeline-event__meta">
                    {formatYearMonth(ev.yearMonth)}
                    {!ev.isPast && <span className="timeline-event__tag">目標</span>}
                  </span>
                  <span className="timeline-event__type">{EVENT_LABEL[ev.type]}</span>
                </div>
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  );
}
