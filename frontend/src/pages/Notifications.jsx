
import { useState } from 'react';
import { DotLottieReact } from '@lottiefiles/dotlottie-react';

import api, { errMsg } from '../api.js';
import {
  Card,
  Msg,
  Spinner,
  useLoad
} from '../components/ui.jsx';

import './Notifications.css';

const NOTIFICATION_META = {
  OVERUSE: {
    icon: '⚠️',
    label: 'High water usage',
    className: 'overuse'
  },
  LEAK: {
    icon: '🚨',
    label: 'Leak alert',
    className: 'leak'
  },
  BILL: {
    icon: '🧾',
    label: 'Billing notification',
    className: 'bill'
  }
};

const getNotificationDate = (value) => {
  if (!value) return 'Date unavailable';

  const date = new Date(value);

  if (Number.isNaN(date.getTime())) {
    return 'Date unavailable';
  }

  return date.toLocaleString('en-IN', {
    day: 'numeric',
    month: 'short',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit'
  });
};

export default function Notifications() {
  const { data, loading, reload } = useLoad('/notifications');

  const [message, setMessage] = useState({
    kind: '',
    text: ''
  });

  const [markingAll, setMarkingAll] = useState(false);
  const [markingIds, setMarkingIds] = useState([]);

  const notifications = Array.isArray(data) ? data : [];

  const unreadCount = notifications.filter(
    (notification) => !notification.read
  ).length;

  const readCount = notifications.length - unreadCount;

  const markAll = async () => {
    if (markingAll || unreadCount === 0) return;

    setMarkingAll(true);
    setMessage({ kind: '', text: '' });

    try {
      await api.post('/notifications/read-all');
      await reload();

      setMessage({
        kind: 'ok',
        text: 'All notifications have been marked as read.'
      });
    } catch (error) {
      setMessage({
        kind: 'error',
        text: errMsg(error)
      });
    } finally {
      setMarkingAll(false);
    }
  };

  const markAsRead = async (notification) => {
    if (notification.read || markingIds.includes(notification.id)) {
      return;
    }

    setMarkingIds((previous) => [
      ...previous,
      notification.id
    ]);

    setMessage({ kind: '', text: '' });

    try {
      await api.post(`/notifications/${notification.id}/read`);
      await reload();
    } catch (error) {
      setMessage({
        kind: 'error',
        text: errMsg(error)
      });
    } finally {
      setMarkingIds((previous) =>
        previous.filter((id) => id !== notification.id)
      );
    }
  };

  if (loading && !data) {
    return (
      <div className="notifications-loading">
        <Spinner />
        <p>Loading your notifications...</p>
      </div>
    );
  }

  return (
    <div className="notifications-page">

      {/* =====================================
          HERO
      ====================================== */}

      <section className="notifications-hero">

        <div className="notifications-hero-content">

          <div className="notifications-eyebrow">
            <span className="notifications-status-dot" />
            SMART WATER ALERT CENTER
          </div>

          <h1>
            Alerts &amp; <span>Notifications</span>
          </h1>

          <p>
            Stay informed about water consumption, possible leaks,
            billing updates and important activities in your
            building.
          </p>

          <div className="notifications-hero-tags">
            <span>Real-time Updates</span>
            <span>Water Monitoring</span>
            <span>Important Alerts</span>
          </div>

        </div>

        <div className="notifications-hero-animation">
          <DotLottieReact
            src="/animations/security-alert.lottie"
            loop
            autoplay
          />
        </div>

      </section>

      {/* =====================================
          SUMMARY
      ====================================== */}

      <section className="notifications-stats">

        <div className="notification-stat-card">

          <div className="notification-stat-icon blue">
            <span>🔔</span>
          </div>

          <div>
            <p>Total Notifications</p>
            <h3>{notifications.length}</h3>
            <span>All received updates</span>
          </div>

        </div>

        <div className="notification-stat-card">

          <div className="notification-stat-icon orange">
            <span>●</span>
          </div>

          <div>
            <p>Unread Notifications</p>
            <h3>{unreadCount}</h3>
            <span>Require your attention</span>
          </div>

        </div>

        <div className="notification-stat-card">

          <div className="notification-stat-icon green">
            <span>✓</span>
          </div>

          <div>
            <p>Read Notifications</p>
            <h3>{readCount}</h3>
            <span>Already reviewed</span>
          </div>

        </div>

      </section>

      {/* =====================================
          NOTIFICATION LIST
      ====================================== */}

      <section className="notifications-content">

        <Card
          right={
            unreadCount > 0 && (
              <button
                type="button"
                className="notifications-mark-all"
                disabled={markingAll}
                onClick={markAll}
              >
                {markingAll ? 'Updating...' : '✓ Mark all as read'}
              </button>
            )
          }
        >

          <div className="notifications-section-heading">

            <div>
              <span className="notifications-section-label">
                YOUR ACTIVITY
              </span>

              <h2>Notification History</h2>

              <p>
                Review your latest alerts and building updates.
              </p>
            </div>

            <div className="notifications-count">
              {unreadCount} Unread
            </div>

          </div>

          <Msg kind={message.kind}>
            {message.text}
          </Msg>

          {loading && (
            <div className="notifications-refreshing">
              <Spinner />
              <span>Refreshing notifications...</span>
            </div>
          )}

          {!notifications.length ? (

            <div className="notifications-empty">

              <div className="notifications-empty-animation">
                <DotLottieReact
                  src="/animations/alert-success.lottie"
                  loop
                  autoplay
                />
              </div>

              <h3>You're all caught up!</h3>

              <p>
                There are no notifications at the moment.
                New water alerts and billing updates will appear here.
              </p>

            </div>

          ) : (

            <div className="notifications-list">

              {notifications.map((notification) => {
                const meta =
                  NOTIFICATION_META[notification.type] || {
                    icon: '🔔',
                    label: 'General notification',
                    className: 'general'
                  };

                const isMarking = markingIds.includes(
                  notification.id
                );

                return (
                  <article
                    key={notification.id}
                    className={[
                      'notification-item',
                      !notification.read ? 'unread' : 'read',
                      meta.className
                    ].join(' ')}
                  >

                    <div className="notification-type-icon">
                      <span>{meta.icon}</span>
                    </div>

                    <div className="notification-details">

                      <div className="notification-title-row">

                        <span className="notification-type-label">
                          {meta.label}
                        </span>

                        {!notification.read && (
                          <span className="notification-unread-label">
                            NEW
                          </span>
                        )}

                      </div>

                      <p className="notification-message">
                        {notification.message}
                      </p>

                      <div className="notification-meta">
                        <span>
                          <span className="notification-clock">◷</span>
                          {getNotificationDate(notification.createdAt)}
                        </span>

                        <span className="notification-status">
                          {notification.read ? 'Read' : 'Unread'}
                        </span>
                      </div>

                    </div>

                    {!notification.read && (
                      <button
                        type="button"
                        className="notification-mark-button"
                        disabled={isMarking}
                        onClick={() => markAsRead(notification)}
                        aria-label="Mark notification as read"
                      >
                        {isMarking ? '...' : '✓'}
                      </button>
                    )}

                  </article>
                );
              })}

            </div>

          )}

        </Card>

      </section>

      {/* =====================================
          FOOTER INFORMATION
      ====================================== */}

      <div className="notifications-footer-note">

        <span>🔒</span>

        <p>
          Notifications help you stay updated about your building's
          water usage, leak alerts and billing activity. Check this
          page regularly to avoid missing important updates.
        </p>

      </div>

    </div>
  );
}
