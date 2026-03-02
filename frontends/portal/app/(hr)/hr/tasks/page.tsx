import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Button } from '@/components/ui/button'

export default function HRTasksPage() {
  const tasks = [
    { id: 1, title: 'Complete Q4 Performance Review', description: 'Submit self-assessment and goals for Q4 review cycle', dueDate: '2024-12-15', priority: 'high', status: 'pending', workflow: 'Performance Review 2024' },
    { id: 2, title: 'Update Emergency Contact Information', description: 'Verify and update emergency contact details in the system', dueDate: '2024-11-30', priority: 'medium', status: 'pending', workflow: 'Annual Information Update' },
    { id: 3, title: 'Complete Compliance Training', description: 'Complete mandatory compliance and security training modules', dueDate: '2024-11-25', priority: 'high', status: 'in_progress', workflow: 'Compliance Training 2024' },
  ]

  const taskStats = [
    { label: 'Pending', count: 2, color: 'text-yellow-500' },
    { label: 'In Progress', count: 1, color: 'text-blue-500' },
    { label: 'Completed This Week', count: 5, color: 'text-green-500' },
  ]

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold">My Tasks</h1>
          <p className="mt-1 text-sm text-muted-foreground">Workflow tasks assigned to you</p>
        </div>
      </div>

      <div className="grid grid-cols-1 gap-4 sm:grid-cols-3">
        {taskStats.map((stat) => (
          <Card key={stat.label}>
            <CardContent className="p-4 flex items-center">
              <div>
                <p className="text-sm font-medium text-muted-foreground">{stat.label}</p>
                <p className="text-2xl font-semibold">{stat.count}</p>
              </div>
            </CardContent>
          </Card>
        ))}
      </div>

      <Card>
        <CardContent className="p-0">
          <ul className="divide-y">
            {tasks.map((task) => (
              <li key={task.id} className="p-6 hover:bg-muted/50">
                <div className="flex items-start justify-between">
                  <div className="flex-1">
                    <div className="flex items-center space-x-3">
                      <h3 className="text-lg font-medium">{task.title}</h3>
                      <span className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium ${
                        task.priority === 'high' ? 'bg-red-100 text-red-800' : 'bg-yellow-100 text-yellow-800'
                      }`}>
                        {task.priority}
                      </span>
                      <span className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium ${
                        task.status === 'pending' ? 'bg-gray-100 text-gray-800' : 'bg-blue-100 text-blue-800'
                      }`}>
                        {task.status}
                      </span>
                    </div>
                    <p className="mt-2 text-sm text-muted-foreground">{task.description}</p>
                    <p className="mt-2 text-sm text-muted-foreground">
                      Due: {task.dueDate} &middot; <span className="text-primary">{task.workflow}</span>
                    </p>
                  </div>
                  <Button className="ml-4" size="sm">Open Task</Button>
                </div>
              </li>
            ))}
          </ul>
        </CardContent>
      </Card>
    </div>
  )
}
