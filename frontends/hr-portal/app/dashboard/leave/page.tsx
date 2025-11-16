import { Calendar, CheckCircle2, Clock, XCircle } from 'lucide-react';

export default function LeavePage() {
  const leaveBalance = {
    annual: { total: 20, used: 5, remaining: 15 },
    sick: { total: 10, used: 2, remaining: 8 },
    personal: { total: 5, used: 1, remaining: 4 },
  };

  const leaveRequests = [
    {
      id: 1,
      type: 'Annual Leave',
      startDate: '2024-12-20',
      endDate: '2024-12-27',
      days: 6,
      status: 'approved',
      reason: 'Year-end vacation',
      approver: 'John Smith',
    },
    {
      id: 2,
      type: 'Sick Leave',
      startDate: '2024-11-05',
      endDate: '2024-11-06',
      days: 2,
      status: 'approved',
      reason: 'Medical appointment',
      approver: 'John Smith',
    },
    {
      id: 3,
      type: 'Personal Leave',
      startDate: '2024-11-18',
      endDate: '2024-11-18',
      days: 1,
      status: 'pending',
      reason: 'Family matter',
      approver: 'Pending approval',
    },
  ];

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold text-gray-900">Leave Management</h1>
          <p className="mt-1 text-sm text-gray-600">
            View your leave balance and submit requests
          </p>
        </div>
        <button className="px-4 py-2 text-sm font-medium text-white bg-blue-600 rounded-md hover:bg-blue-700">
          Request Leave
        </button>
      </div>

      {/* Leave Balance */}
      <div className="grid grid-cols-1 gap-4 sm:grid-cols-3">
        <div className="bg-white p-6 rounded-lg shadow">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-sm font-medium text-gray-600">Annual Leave</p>
              <p className="mt-2 text-3xl font-semibold text-gray-900">
                {leaveBalance.annual.remaining}
              </p>
              <p className="text-sm text-gray-500">
                of {leaveBalance.annual.total} days remaining
              </p>
            </div>
            <Calendar className="h-10 w-10 text-blue-600" />
          </div>
          <div className="mt-4">
            <div className="w-full bg-gray-200 rounded-full h-2">
              <div
                className="bg-blue-600 h-2 rounded-full"
                style={{
                  width: `${(leaveBalance.annual.remaining / leaveBalance.annual.total) * 100}%`,
                }}
              ></div>
            </div>
          </div>
        </div>

        <div className="bg-white p-6 rounded-lg shadow">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-sm font-medium text-gray-600">Sick Leave</p>
              <p className="mt-2 text-3xl font-semibold text-gray-900">
                {leaveBalance.sick.remaining}
              </p>
              <p className="text-sm text-gray-500">
                of {leaveBalance.sick.total} days remaining
              </p>
            </div>
            <Calendar className="h-10 w-10 text-red-600" />
          </div>
          <div className="mt-4">
            <div className="w-full bg-gray-200 rounded-full h-2">
              <div
                className="bg-red-600 h-2 rounded-full"
                style={{
                  width: `${(leaveBalance.sick.remaining / leaveBalance.sick.total) * 100}%`,
                }}
              ></div>
            </div>
          </div>
        </div>

        <div className="bg-white p-6 rounded-lg shadow">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-sm font-medium text-gray-600">Personal Leave</p>
              <p className="mt-2 text-3xl font-semibold text-gray-900">
                {leaveBalance.personal.remaining}
              </p>
              <p className="text-sm text-gray-500">
                of {leaveBalance.personal.total} days remaining
              </p>
            </div>
            <Calendar className="h-10 w-10 text-green-600" />
          </div>
          <div className="mt-4">
            <div className="w-full bg-gray-200 rounded-full h-2">
              <div
                className="bg-green-600 h-2 rounded-full"
                style={{
                  width: `${(leaveBalance.personal.remaining / leaveBalance.personal.total) * 100}%`,
                }}
              ></div>
            </div>
          </div>
        </div>
      </div>

      {/* Leave Requests */}
      <div className="bg-white shadow rounded-lg">
        <div className="px-6 py-5 border-b border-gray-200">
          <h2 className="text-lg font-medium text-gray-900">Leave Requests</h2>
        </div>
        <ul className="divide-y divide-gray-200">
          {leaveRequests.map((request) => (
            <li key={request.id} className="p-6 hover:bg-gray-50">
              <div className="flex items-center justify-between">
                <div className="flex-1">
                  <div className="flex items-center space-x-3">
                    <h3 className="text-lg font-medium text-gray-900">
                      {request.type}
                    </h3>
                    <span
                      className={`inline-flex items-center px-3 py-0.5 rounded-full text-xs font-medium ${
                        request.status === 'approved'
                          ? 'bg-green-100 text-green-800'
                          : request.status === 'pending'
                          ? 'bg-yellow-100 text-yellow-800'
                          : 'bg-red-100 text-red-800'
                      }`}
                    >
                      {request.status === 'approved' && <CheckCircle2 className="h-3 w-3 mr-1" />}
                      {request.status === 'pending' && <Clock className="h-3 w-3 mr-1" />}
                      {request.status === 'rejected' && <XCircle className="h-3 w-3 mr-1" />}
                      {request.status}
                    </span>
                  </div>
                  <div className="mt-2 grid grid-cols-2 gap-4 text-sm text-gray-600">
                    <div>
                      <span className="font-medium">Dates:</span>{' '}
                      {new Date(request.startDate).toLocaleDateString()} -{' '}
                      {new Date(request.endDate).toLocaleDateString()}
                    </div>
                    <div>
                      <span className="font-medium">Duration:</span> {request.days} days
                    </div>
                    <div>
                      <span className="font-medium">Reason:</span> {request.reason}
                    </div>
                    <div>
                      <span className="font-medium">Approver:</span> {request.approver}
                    </div>
                  </div>
                </div>
              </div>
            </li>
          ))}
        </ul>
      </div>
    </div>
  );
}
