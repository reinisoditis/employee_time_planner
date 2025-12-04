import React, { useState } from 'react';

function ShiftList({ shifts, onAddShift }) {
  const [startTime, setStartTime] = useState('');
  const [endTime, setEndTime] = useState('');
  const [role, setRole] = useState('');

  const handleSubmit = (e) => {
    e.preventDefault();
    if (startTime && endTime && role) {
      onAddShift({
        startTime: startTime,
        endTime: endTime,
        requiredRole: role
      });
      setStartTime('');
      setEndTime('');
      setRole('');
    }
  };

  const formatDateTime = (dateTime) => {
    if (!dateTime) return '';
    return new Date(dateTime).toLocaleString();
  };

  return (
    <div className="card">
      <div className="card-header bg-info text-white">
        <h5 className="mb-0">Shifts</h5>
      </div>
      <div className="card-body">
        <form onSubmit={handleSubmit} className="mb-3">
          <div className="mb-2">
            <input
              type="datetime-local"
              className="form-control"
              placeholder="Start Time"
              value={startTime}
              onChange={(e) => setStartTime(e.target.value)}
            />
          </div>
          <div className="mb-2">
            <input
              type="datetime-local"
              className="form-control"
              placeholder="End Time"
              value={endTime}
              onChange={(e) => setEndTime(e.target.value)}
            />
          </div>
          <div className="mb-2">
            <input
              type="text"
              className="form-control"
              placeholder="Required Role"
              value={role}
              onChange={(e) => setRole(e.target.value)}
            />
          </div>
          <button className="btn btn-info text-white w-100" type="submit">
            Add Shift
          </button>
        </form>
        <div className="list-group" style={{ maxHeight: '300px', overflowY: 'auto' }}>
          {shifts.length === 0 ? (
            <div className="text-muted text-center py-3">No shifts added yet</div>
          ) : (
            shifts.map((shift) => (
              <div key={shift.id} className="list-group-item">
                <strong>{shift.requiredRole}</strong>
                <br />
                <small className="text-muted">
                  {formatDateTime(shift.startTime)} - {formatDateTime(shift.endTime)}
                </small>
              </div>
            ))
          )}
        </div>
      </div>
    </div>
  );
}

export default ShiftList;
