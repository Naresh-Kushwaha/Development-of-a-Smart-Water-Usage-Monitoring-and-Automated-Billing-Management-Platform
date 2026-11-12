
import { useState } from 'react';

import { DotLottieReact } from '@lottiefiles/dotlottie-react';

import api, { errMsg } from '../../api.js';

import {
  Card,
  Field,
  Msg,
  Spinner,
  useLoad,
} from '../../components/ui.jsx';

import './Households.css';

const EMPTY = {
  unitNumber: '',
  areaSqft: '',
  occupants: 2,
  meterNumber: '',
};

// Reusable Lottie component
function HouseholdAnimation({ src, className = '' }) {
  return (
    <div className={className}>
      <DotLottieReact
        src={src}
        loop
        autoplay
      />
    </div>
  );
}

export default function Households() {
  const { data, loading, reload } = useLoad('/admin/households');

  const [f, setF] = useState(EMPTY);
  const [editId, setEditId] = useState(null);

  const [err, setErr] = useState('');
  const [success, setSuccess] = useState('');

  const [saving, setSaving] = useState(false);

  // Handle form input changes
  const set = (key) => (e) => {
    setF({
      ...f,
      [key]: e.target.value,
    });
  };

  // Reset form
  const resetForm = () => {
    setEditId(null);
    setF(EMPTY);
    setErr('');
    setSuccess('');
  };

  // Register or update household
  const save = async (e) => {
    e.preventDefault();

    setErr('');
    setSuccess('');
    setSaving(true);

    const body = {
      ...f,
      areaSqft: Number(f.areaSqft),
      occupants: Number(f.occupants),
    };

    try {
      if (editId) {
        await api.put(`/admin/households/${editId}`, body);

        setSuccess('Household updated successfully!');
      } else {
        await api.post('/admin/households', body);

        setSuccess('New household registered successfully!');
      }

      setF(EMPTY);
      setEditId(null);

      reload();
    } catch (ex) {
      setErr(errMsg(ex));
    } finally {
      setSaving(false);
    }
  };

  // Edit household
  const edit = (h) => {
    setEditId(h.id);

    setF({
      unitNumber: h.unitNumber,
      areaSqft: h.areaSqft,
      occupants: h.occupants,
      meterNumber: h.meterNumber ?? '',
    });

    setErr('');
    setSuccess('');

    window.scrollTo({
      top: 0,
      behavior: 'smooth',
    });
  };

  // Delete household
  const del = async (h) => {
    if (
      !confirm(
        `Delete unit ${h.unitNumber}? Its usage logs and invoices will be removed.`
      )
    ) {
      return;
    }

    setErr('');
    setSuccess('');

    try {
      await api.delete(`/admin/households/${h.id}`);

      setSuccess(`Unit ${h.unitNumber} deleted successfully!`);

      if (editId === h.id) {
        resetForm();
      }

      reload();
    } catch (ex) {
      setErr(errMsg(ex));
    }
  };

  // Assign resident
  const assign = async (h) => {
    const email = prompt(
      `Assign a registered resident to ${h.unitNumber} (email):`
    );

    if (!email?.trim()) return;

    setErr('');
    setSuccess('');

    try {
      await api.post(`/admin/households/${h.id}/assign`, {
        email: email.trim(),
      });

      setSuccess(`Resident assigned to unit ${h.unitNumber}.`);

      reload();
    } catch (ex) {
      setErr(errMsg(ex));
    }
  };

  const households = data || [];

  const meteredCount = households.filter((h) => h.metered).length;

  const unmeteredCount = households.length - meteredCount;

  return (
    <div className="households-page">

      {/* Hero Section */}
      <section className="households-hero">
        <div className="households-hero-content">
          <span className="households-eyebrow">
            APARTMENT MANAGEMENT
          </span>

          <h2>Households &amp; meters</h2>

          <p>
            Manage apartment units, register water meters,
            and assign residents from one central place.
          </p>

          <div className="households-hero-stats">
            <div>
              <strong>{households.length}</strong>
              <span>Total units</span>
            </div>

            <div>
              <strong>{meteredCount}</strong>
              <span>Metered</span>
            </div>

            <div>
              <strong>{unmeteredCount}</strong>
              <span>Unmetered</span>
            </div>
          </div>
        </div>

        <HouseholdAnimation
          src="/animations/smart-building.lottie"
          className="households-hero-animation"
        />
      </section>

      {/* Register / Edit Household */}
      <Card title={editId ? 'Edit household' : 'Register household'}>
        <div className="households-form-header">
          <div>
            <p className="muted">
              {editId
                ? 'Update the unit information and meter details.'
                : 'Add a new apartment unit and configure its water meter.'}
            </p>
          </div>

          <HouseholdAnimation
            src="/animations/house-registration.lottie"
            className="households-form-animation"
          />
        </div>

        <form className="inline" onSubmit={save}>

          <Field label="Unit no.">
            <input
              required
              value={f.unitNumber}
              onChange={set('unitNumber')}
              placeholder="e.g. A-101"
            />
          </Field>

          <Field label="Area (sq ft)">
            <input
              type="number"
              min="1"
              step="0.1"
              required
              value={f.areaSqft}
              onChange={set('areaSqft')}
              placeholder="e.g. 1200"
            />
          </Field>

          <Field label="Occupants">
            <input
              type="number"
              min="1"
              required
              value={f.occupants}
              onChange={set('occupants')}
            />
          </Field>

          <Field
            label="Meter no."
            hint="Blank = unmetered (billed by area)"
          >
            <input
              value={f.meterNumber}
              onChange={set('meterNumber')}
              placeholder="Enter meter number"
            />
          </Field>

          <div className="households-form-actions">
            <button
              className="primary"
              type="submit"
              disabled={saving}
            >
              {saving
                ? 'Saving...'
                : editId
                  ? 'Update household'
                  : 'Add household'}
            </button>

            {editId && (
              <button
                type="button"
                className="ghost"
                onClick={resetForm}
              >
                Cancel
              </button>
            )}
          </div>
        </form>

        {err && <Msg kind="error">{err}</Msg>}

        {success && (
          <div className="households-success">
            <HouseholdAnimation
              src="/animations/house-success.lottie"
              className="households-success-animation"
            />

            <Msg kind="ok">{success}</Msg>
          </div>
        )}
      </Card>

      {/* Household List */}
      <Card title={`Registered units (${households.length})`}>

        {loading && !data ? (
          <div className="households-loading">
            <HouseholdAnimation
              src="/animations/meter-loading.lottie"
              className="households-loading-animation"
            />

            <p>Loading household information...</p>
          </div>
        ) : households.length === 0 ? (
          <div className="households-empty">
            <HouseholdAnimation
              src="/animations/empty-building.lottie"
              className="households-empty-animation"
            />

            <h3>No households registered yet</h3>

            <p className="muted">
              Start by registering your first apartment unit.
            </p>
          </div>
        ) : (
          <div className="table-wrap households-table-wrap">
            <table>
              <thead>
                <tr>
                  <th>Unit</th>
                  <th className="r">Area</th>
                  <th className="r">People</th>
                  <th>Meter</th>
                  <th>Residents</th>
                  <th>Actions</th>
                </tr>
              </thead>

              <tbody>
                {households.map((h) => (
                  <tr key={h.id}>

                    <td>
                      <strong>{h.unitNumber}</strong>
                    </td>

                    <td className="r">
                      {h.areaSqft}
                    </td>

                    <td className="r">
                      {h.occupants}
                    </td>

                    <td>
                      {h.metered ? (
                        <span className="households-meter">
                          <span className="households-meter-dot" />
                          {h.meterNumber}
                        </span>
                      ) : (
                        <span className="pill preview">
                          UNMETERED
                        </span>
                      )}
                    </td>

                    <td>
                      {h.residents?.length > 0
                        ? h.residents.join(', ')
                        : <span className="muted">—</span>}
                    </td>

                    <td className="actions households-actions">
                      <button
                        className="link"
                        type="button"
                        onClick={() => assign(h)}
                      >
                        Assign
                      </button>

                      <button
                        className="link"
                        type="button"
                        onClick={() => edit(h)}
                      >
                        Edit
                      </button>

                      <button
                        className="link danger"
                        type="button"
                        onClick={() => del(h)}
                      >
                        Delete
                      </button>
                    </td>

                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </Card>

    </div>
  );
}
