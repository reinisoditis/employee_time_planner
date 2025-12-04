import React, { useState, useEffect } from 'react';
import axios from 'axios';
import EmployeeList from './components/EmployeeList';
import ShiftList from './components/ShiftList';
import ScheduleView from './components/ScheduleView';

const API_BASE_URL = '/api';

function App() {
  const [employees, setEmployees] = useState([]);
  const [shifts, setShifts] = useState([]);
  const [schedule, setSchedule] = useState(null);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    fetchEmployees();
    fetchShifts();
  }, []);

  const fetchEmployees = async () => {
    try {
      const response = await axios.get(`${API_BASE_URL}/employees`);
      setEmployees(response.data);
    } catch (error) {
      console.error('Error fetching employees:', error);
    }
  };

  const fetchShifts = async () => {
    try {
      const response = await axios.get(`${API_BASE_URL}/shifts`);
      setShifts(response.data);
    } catch (error) {
      console.error('Error fetching shifts:', error);
    }
  };

  const addEmployee = async (name) => {
    try {
      await axios.post(`${API_BASE_URL}/employees`, { name });
      fetchEmployees();
    } catch (error) {
      console.error('Error adding employee:', error);
    }
  };

  const addShift = async (shift) => {
    try {
      await axios.post(`${API_BASE_URL}/shifts`, shift);
      fetchShifts();
    } catch (error) {
      console.error('Error adding shift:', error);
    }
  };

  const solveSchedule = async () => {
    setLoading(true);
    try {
      const response = await axios.post(`${API_BASE_URL}/solve`);
      setSchedule(response.data);
    } catch (error) {
      console.error('Error solving schedule:', error);
      alert('Error solving schedule. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="App">
      <nav className="navbar navbar-dark bg-primary mb-4">
        <div className="container-fluid">
          <span className="navbar-brand mb-0 h1">Employee Time Planner</span>
        </div>
      </nav>

      <div className="container">
        <div className="row">
          <div className="col-md-6 mb-4">
            <EmployeeList employees={employees} onAddEmployee={addEmployee} />
          </div>
          <div className="col-md-6 mb-4">
            <ShiftList shifts={shifts} onAddShift={addShift} />
          </div>
        </div>

        <div className="row mb-4">
          <div className="col-12 text-center">
            <button 
              className="btn btn-success btn-lg" 
              onClick={solveSchedule}
              disabled={loading || employees.length === 0 || shifts.length === 0}
            >
              {loading ? (
                <>
                  <span className="spinner-border spinner-border-sm me-2" role="status" aria-hidden="true"></span>
                  Solving...
                </>
              ) : (
                'Solve Schedule'
              )}
            </button>
          </div>
        </div>

        {schedule && (
          <div className="row">
            <div className="col-12">
              <ScheduleView schedule={schedule} />
            </div>
          </div>
        )}
      </div>
    </div>
  );
}

export default App;
