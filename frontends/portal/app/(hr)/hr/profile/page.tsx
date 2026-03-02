import { auth } from '@/auth'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'

export default async function ProfilePage() {
  const session = await auth()

  const employee = {
    name: session?.user?.name || 'Employee Name',
    email: session?.user?.email || 'employee@werkflow.com',
    employeeId: 'EMP001',
    jobTitle: 'Senior Software Engineer',
    department: 'Engineering',
    organization: 'Werkflow Corporation',
    manager: 'John Smith',
    phone: '+1 (555) 123-4567',
    location: 'New York, NY',
    hireDate: '2022-01-15',
    address: '123 Main Street, New York, NY 10001',
  }

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-3xl font-bold">My Profile</h1>
        <p className="mt-1 text-sm text-muted-foreground">View and manage your personal information</p>
      </div>

      <Card>
        <div className="h-32 bg-gradient-to-r from-blue-500 to-blue-600 rounded-t-lg" />
        <CardContent className="pb-6">
          <div className="flex items-end -mt-16">
            <div className="h-32 w-32 rounded-full border-4 border-background bg-background flex items-center justify-center shadow-lg">
              <span className="text-5xl font-bold text-primary">{employee.name.charAt(0)}</span>
            </div>
            <div className="ml-6 flex-1 mt-16">
              <h2 className="text-2xl font-bold">{employee.name}</h2>
              <p className="text-muted-foreground">{employee.jobTitle}</p>
            </div>
            <button className="mt-16 px-4 py-2 text-sm font-medium border rounded-md hover:bg-muted">
              Edit Profile
            </button>
          </div>
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle>Personal Information</CardTitle>
        </CardHeader>
        <CardContent>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
            <InfoItem label="Email" value={employee.email} />
            <InfoItem label="Phone" value={employee.phone} />
            <InfoItem label="Location" value={employee.location} />
            <InfoItem label="Address" value={employee.address} />
          </div>
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle>Employment Information</CardTitle>
        </CardHeader>
        <CardContent>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
            <InfoItem label="Employee ID" value={employee.employeeId} />
            <InfoItem label="Hire Date" value={employee.hireDate} />
            <InfoItem label="Department" value={employee.department} />
            <InfoItem label="Organization" value={employee.organization} />
            <InfoItem label="Reports To" value={employee.manager} />
          </div>
        </CardContent>
      </Card>
    </div>
  )
}

function InfoItem({ label, value }: { label: string; value: string }) {
  return (
    <div>
      <p className="text-sm font-medium text-muted-foreground">{label}</p>
      <p className="mt-1 text-sm">{value}</p>
    </div>
  )
}
