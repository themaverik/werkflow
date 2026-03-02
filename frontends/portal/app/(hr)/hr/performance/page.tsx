import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'

export default function PerformancePage() {
  const currentReview = {
    period: 'Q4 2024',
    dueDate: '2024-12-15',
    progress: 60,
  }

  const goals = [
    { id: 1, title: 'Complete Backend API Migration', category: 'Technical', progress: 85, status: 'on_track', dueDate: '2024-12-31' },
    { id: 2, title: 'Mentor 2 Junior Developers', category: 'Leadership', progress: 50, status: 'on_track', dueDate: '2024-12-31' },
    { id: 3, title: 'Reduce Production Incidents by 30%', category: 'Quality', progress: 70, status: 'on_track', dueDate: '2024-12-31' },
  ]

  const previousReviews = [
    { id: 1, period: 'Q3 2024', rating: 4.5, completedDate: '2024-09-30' },
    { id: 2, period: 'Q2 2024', rating: 4.2, completedDate: '2024-06-30' },
  ]

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-3xl font-bold">Performance Reviews</h1>
        <p className="mt-1 text-sm text-muted-foreground">Track your goals and review history</p>
      </div>

      <div className="bg-gradient-to-r from-blue-500 to-blue-600 rounded-lg shadow-lg p-6 text-white">
        <div className="flex items-center justify-between">
          <div className="flex-1">
            <h2 className="text-2xl font-bold">{currentReview.period} Review</h2>
            <p className="mt-2 text-blue-100">Due: {currentReview.dueDate}</p>
            <div className="mt-4">
              <div className="flex items-center justify-between text-sm mb-2">
                <span>Overall Progress</span>
                <span>{currentReview.progress}%</span>
              </div>
              <div className="w-full bg-blue-400 rounded-full h-2">
                <div className="bg-white h-2 rounded-full" style={{ width: `${currentReview.progress}%` }} />
              </div>
            </div>
          </div>
          <button className="ml-6 px-6 py-3 bg-white text-blue-600 rounded-md font-medium hover:bg-blue-50">
            Continue Review
          </button>
        </div>
      </div>

      <Card>
        <CardHeader>
          <CardTitle>Current Goals</CardTitle>
        </CardHeader>
        <CardContent>
          <ul className="divide-y">
            {goals.map((goal) => (
              <li key={goal.id} className="py-4">
                <div className="flex items-center space-x-3 mb-2">
                  <h3 className="text-lg font-medium">{goal.title}</h3>
                  <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-blue-100 text-blue-800">
                    {goal.category}
                  </span>
                </div>
                <div className="flex items-center justify-between text-sm mb-2">
                  <span className="text-muted-foreground">Progress</span>
                  <span className="font-medium">{goal.progress}%</span>
                </div>
                <div className="w-full bg-muted rounded-full h-2">
                  <div
                    className={`h-2 rounded-full ${goal.status === 'on_track' ? 'bg-green-500' : 'bg-yellow-500'}`}
                    style={{ width: `${goal.progress}%` }}
                  />
                </div>
                <p className="mt-2 text-sm text-muted-foreground">Due: {goal.dueDate}</p>
              </li>
            ))}
          </ul>
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle>Review History</CardTitle>
        </CardHeader>
        <CardContent>
          <ul className="divide-y">
            {previousReviews.map((review) => (
              <li key={review.id} className="py-4 flex items-center justify-between">
                <div>
                  <h3 className="text-lg font-medium">{review.period}</h3>
                  <p className="text-sm text-muted-foreground">Completed on {review.completedDate}</p>
                </div>
                <div className="flex items-center space-x-2">
                  <span className="text-2xl font-bold">{review.rating}</span>
                  <span className="text-muted-foreground">/ 5</span>
                </div>
              </li>
            ))}
          </ul>
        </CardContent>
      </Card>
    </div>
  )
}
