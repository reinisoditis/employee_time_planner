import React from 'react';

function ScheduleView({ schedule }) {
  const formatDateTime = (dateTime) => {
    if (!dateTime) return '';
    return new Date(dateTime).toLocaleString();
  };

  const getScoreClass = (score) => {
    if (!score) return 'secondary';
    const hardScore = score.hardScore || 0;
    const softScore = score.softScore || 0;
    
    if (hardScore < 0) return 'danger';
    if (softScore < 0) return 'warning';
    return 'success';
  };

  const assignedShifts = schedule.shifts.filter(shift => shift.employee);
  const unassignedShifts = schedule.shifts.filter(shift => !shift.employee);

  return (
    <div className="card">
      <div className="card-header bg-success text-white">
        <h5 className="mb-0">Optimized Schedule</h5>
      </div>
      <div className="card-body">
        {schedule.score && (
          <div className={`alert alert-${getScoreClass(schedule.score)} mb-3`}>
            <strong>Score:</strong> {schedule.score.hardScore || 0} hard / {schedule.score.softScore || 0} soft
          </div>
        )}

        {assignedShifts.length > 0 && (
          <div className="mb-4">
            <h6>Assigned Shifts</h6>
            <div className="table-responsive">
              <table className="table table-striped">
                <thead>
                  <tr>
                    <th>Employee</th>
                    <th>Role</th>
                    <th>Start Time</th>
                    <th>End Time</th>
                  </tr>
                </thead>
                <tbody>
                  {assignedShifts.map((shift) => (
                    <tr key={shift.id}>
                      <td>
                        <span className="badge bg-primary">{shift.employee.name}</span>
                      </td>
                      <td>{shift.requiredRole}</td>
                      <td>{formatDateTime(shift.startTime)}</td>
                      <td>{formatDateTime(shift.endTime)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        )}

        {unassignedShifts.length > 0 && (
          <div>
            <h6 className="text-warning">Unassigned Shifts</h6>
            <div className="table-responsive">
              <table className="table table-striped">
                <thead>
                  <tr>
                    <th>Role</th>
                    <th>Start Time</th>
                    <th>End Time</th>
                  </tr>
                </thead>
                <tbody>
                  {unassignedShifts.map((shift) => (
                    <tr key={shift.id}>
                      <td>{shift.requiredRole}</td>
                      <td>{formatDateTime(shift.startTime)}</td>
                      <td>{formatDateTime(shift.endTime)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        )}

        {assignedShifts.length === 0 && unassignedShifts.length === 0 && (
          <div className="text-muted text-center py-3">No shifts in schedule</div>
        )}
      </div>
    </div>
  );
}

export default ScheduleView;
