import { CheckCircle2, Circle, Clock } from 'lucide-react';

export default function TasksPage() {
  const tasks = [
    {
      id: 1,
      title: 'Complete Q4 Performance Review',
      description: 'Submit self-assessment and goals for Q4 review cycle',
      dueDate: '2024-12-15',
      priority: 'high',
      status: 'pending',
      workflow: 'Performance Review 2024',
    },
    {
      id: 2,
      title: 'Update Emergency Contact Information',
      description: 'Verify and update emergency contact details in the system',
      dueDate: '2024-11-30',
      priority: 'medium',
      status: 'pending',
      workflow: 'Annual Information Update',
    },
    {
      id: 3,
      title: 'Complete Compliance Training',
      description: 'Complete mandatory compliance and security training modules',
      dueDate: '2024-11-25',
      priority: 'high',
      status: 'in_progress',
      workflow: 'Compliance Training 2024',
    },
  ];

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold text-gray-900">My Tasks</h1>
          <p className="mt-1 text-sm text-gray-600">
            Workflow tasks assigned to you
          </p>
        </div>
        <div className="flex items-center space-x-2">
          <button className="px-4 py-2 text-sm font-medium text-gray-700 bg-white border border-gray-300 rounded-md hover:bg-gray-50">
            Filter
          </button>
          <button className="px-4 py-2 text-sm font-medium text-gray-700 bg-white border border-gray-300 rounded-md hover:bg-gray-50">
            Sort
          </button>
        </div>
      </div>

      {/* Task Stats */}
      <div className="grid grid-cols-1 gap-4 sm:grid-cols-3">
        <div className="bg-white p-4 rounded-lg shadow">
          <div className="flex items-center">
            <Circle className="h-5 w-5 text-yellow-500 mr-2" />
            <div>
              <p className="text-sm font-medium text-gray-600">Pending</p>
              <p className="text-2xl font-semibold text-gray-900">2</p>
            </div>
          </div>
        </div>
        <div className="bg-white p-4 rounded-lg shadow">
          <div className="flex items-center">
            <Clock className="h-5 w-5 text-blue-500 mr-2" />
            <div>
              <p className="text-sm font-medium text-gray-600">In Progress</p>
              <p className="text-2xl font-semibold text-gray-900">1</p>
            </div>
          </div>
        </div>
        <div className="bg-white p-4 rounded-lg shadow">
          <div className="flex items-center">
            <CheckCircle2 className="h-5 w-5 text-green-500 mr-2" />
            <div>
              <p className="text-sm font-medium text-gray-600">Completed This Week</p>
              <p className="text-2xl font-semibold text-gray-900">5</p>
            </div>
          </div>
        </div>
      </div>

      {/* Task List */}
      <div className="bg-white shadow rounded-lg">
        <ul className="divide-y divide-gray-200">
          {tasks.map((task) => (
            <li key={task.id} className="p-6 hover:bg-gray-50">
              <div className="flex items-start justify-between">
                <div className="flex-1">
                  <div className="flex items-center space-x-3">
                    <h3 className="text-lg font-medium text-gray-900">
                      {task.title}
                    </h3>
                    <span
                      className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium ${
                        task.priority === 'high'
                          ? 'bg-red-100 text-red-800'
                          : 'bg-yellow-100 text-yellow-800'
                      }`}
                    >
                      {task.priority}
                    </span>
                    <span
                      className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium ${
                        task.status === 'pending'
                          ? 'bg-gray-100 text-gray-800'
                          : 'bg-blue-100 text-blue-800'
                      }`}
                    >
                      {task.status}
                    </span>
                  </div>
                  <p className="mt-2 text-sm text-gray-600">{task.description}</p>
                  <div className="mt-2 flex items-center text-sm text-gray-500">
                    <Clock className="h-4 w-4 mr-1" />
                    Due: {new Date(task.dueDate).toLocaleDateString()}
                    <span className="mx-2">•</span>
                    <span className="text-blue-600">{task.workflow}</span>
                  </div>
                </div>
                <div className="ml-4">
                  <button className="px-4 py-2 text-sm font-medium text-white bg-blue-600 rounded-md hover:bg-blue-700">
                    Open Task
                  </button>
                </div>
              </div>
            </li>
          ))}
        </ul>
      </div>
    </div>
  );
}
