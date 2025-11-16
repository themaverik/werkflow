import { Star, TrendingUp, Target, Award } from 'lucide-react';

export default function PerformancePage() {
  const currentReview = {
    period: 'Q4 2024',
    status: 'in_progress',
    dueDate: '2024-12-15',
    progress: 60,
  };

  const goals = [
    {
      id: 1,
      title: 'Complete Backend API Migration',
      category: 'Technical',
      progress: 85,
      status: 'on_track',
      dueDate: '2024-12-31',
    },
    {
      id: 2,
      title: 'Mentor 2 Junior Developers',
      category: 'Leadership',
      progress: 50,
      status: 'on_track',
      dueDate: '2024-12-31',
    },
    {
      id: 3,
      title: 'Reduce Production Incidents by 30%',
      category: 'Quality',
      progress: 70,
      status: 'on_track',
      dueDate: '2024-12-31',
    },
  ];

  const previousReviews = [
    {
      id: 1,
      period: 'Q3 2024',
      rating: 4.5,
      status: 'completed',
      completedDate: '2024-09-30',
    },
    {
      id: 2,
      period: 'Q2 2024',
      rating: 4.2,
      status: 'completed',
      completedDate: '2024-06-30',
    },
  ];

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold text-gray-900">Performance Reviews</h1>
          <p className="mt-1 text-sm text-gray-600">
            Track your goals and review history
          </p>
        </div>
      </div>

      {/* Current Review Status */}
      <div className="bg-gradient-to-r from-blue-500 to-blue-600 rounded-lg shadow-lg p-6 text-white">
        <div className="flex items-center justify-between">
          <div className="flex-1">
            <h2 className="text-2xl font-bold">{currentReview.period} Review</h2>
            <p className="mt-2 text-blue-100">
              Due: {new Date(currentReview.dueDate).toLocaleDateString()}
            </p>
            <div className="mt-4">
              <div className="flex items-center justify-between text-sm mb-2">
                <span>Overall Progress</span>
                <span>{currentReview.progress}%</span>
              </div>
              <div className="w-full bg-blue-400 rounded-full h-2">
                <div
                  className="bg-white h-2 rounded-full transition-all"
                  style={{ width: `${currentReview.progress}%` }}
                ></div>
              </div>
            </div>
          </div>
          <div className="ml-6">
            <button className="px-6 py-3 bg-white text-blue-600 rounded-md font-medium hover:bg-blue-50 transition-colors">
              Continue Review
            </button>
          </div>
        </div>
      </div>

      {/* Goals */}
      <div className="bg-white shadow rounded-lg">
        <div className="px-6 py-5 border-b border-gray-200 flex items-center justify-between">
          <h2 className="text-lg font-medium text-gray-900">Current Goals</h2>
          <button className="text-sm text-blue-600 hover:text-blue-700 font-medium">
            View All
          </button>
        </div>
        <ul className="divide-y divide-gray-200">
          {goals.map((goal) => (
            <li key={goal.id} className="p-6">
              <div className="flex items-start justify-between">
                <div className="flex-1">
                  <div className="flex items-center space-x-3">
                    <Target className="h-5 w-5 text-blue-600" />
                    <h3 className="text-lg font-medium text-gray-900">
                      {goal.title}
                    </h3>
                    <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-blue-100 text-blue-800">
                      {goal.category}
                    </span>
                  </div>
                  <div className="mt-4">
                    <div className="flex items-center justify-between text-sm mb-2">
                      <span className="text-gray-600">Progress</span>
                      <span className="font-medium text-gray-900">{goal.progress}%</span>
                    </div>
                    <div className="w-full bg-gray-200 rounded-full h-2">
                      <div
                        className={`h-2 rounded-full transition-all ${
                          goal.status === 'on_track'
                            ? 'bg-green-500'
                            : goal.status === 'at_risk'
                            ? 'bg-yellow-500'
                            : 'bg-red-500'
                        }`}
                        style={{ width: `${goal.progress}%` }}
                      ></div>
                    </div>
                  </div>
                  <p className="mt-2 text-sm text-gray-500">
                    Due: {new Date(goal.dueDate).toLocaleDateString()}
                  </p>
                </div>
              </div>
            </li>
          ))}
        </ul>
      </div>

      {/* Review History */}
      <div className="bg-white shadow rounded-lg">
        <div className="px-6 py-5 border-b border-gray-200">
          <h2 className="text-lg font-medium text-gray-900">Review History</h2>
        </div>
        <ul className="divide-y divide-gray-200">
          {previousReviews.map((review) => (
            <li key={review.id} className="p-6 hover:bg-gray-50">
              <div className="flex items-center justify-between">
                <div className="flex items-center space-x-4">
                  <div className="flex items-center justify-center h-12 w-12 rounded-full bg-green-100">
                    <Award className="h-6 w-6 text-green-600" />
                  </div>
                  <div>
                    <h3 className="text-lg font-medium text-gray-900">
                      {review.period}
                    </h3>
                    <p className="text-sm text-gray-500">
                      Completed on {new Date(review.completedDate).toLocaleDateString()}
                    </p>
                  </div>
                </div>
                <div className="flex items-center space-x-4">
                  <div className="text-right">
                    <div className="flex items-center space-x-1">
                      <Star className="h-5 w-5 text-yellow-400 fill-current" />
                      <span className="text-2xl font-bold text-gray-900">
                        {review.rating}
                      </span>
                      <span className="text-gray-500">/ 5</span>
                    </div>
                    <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-green-100 text-green-800">
                      Completed
                    </span>
                  </div>
                  <button className="px-4 py-2 text-sm font-medium text-gray-700 bg-white border border-gray-300 rounded-md hover:bg-gray-50">
                    View Details
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
