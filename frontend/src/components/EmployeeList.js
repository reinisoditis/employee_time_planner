import React, { useState } from 'react';

function EmployeeList({ employees, onAddEmployee }) {
  const [name, setName] = useState('');

  const handleSubmit = (e) => {
    e.preventDefault();
    if (name.trim()) {
      onAddEmployee(name);
      setName('');
    }
  };

  return (
    <div className="card">
      <div className="card-header bg-primary text-white">
        <h5 className="mb-0">Employees</h5>
      </div>
      <div className="card-body">
        <form onSubmit={handleSubmit} className="mb-3">
          <div className="input-group">
            <input
              type="text"
              className="form-control"
              placeholder="Employee name"
              value={name}
              onChange={(e) => setName(e.target.value)}
            />
            <button className="btn btn-primary" type="submit">
              Add
            </button>
          </div>
        </form>
        <div className="list-group">
          {employees.length === 0 ? (
            <div className="text-muted text-center py-3">No employees added yet</div>
          ) : (
            employees.map((employee) => (
              <div key={employee.id} className="list-group-item">
                {employee.name}
              </div>
            ))
          )}
        </div>
      </div>
    </div>
  );
}

export default EmployeeList;
