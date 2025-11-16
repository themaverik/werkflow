import { auth } from '@/auth';
import { Mail, Phone, MapPin, Briefcase, Calendar, Building2 } from 'lucide-react';

export default async function ProfilePage() {
  const session = await auth();

  // Mock employee data - in real app, fetch from admin service
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
  };

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-3xl font-bold text-gray-900">My Profile</h1>
        <p className="mt-1 text-sm text-gray-600">
          View and manage your personal information
        </p>
      </div>

      {/* Profile Header */}
      <div className="bg-white shadow rounded-lg overflow-hidden">
        <div className="h-32 bg-gradient-to-r from-blue-500 to-blue-600"></div>
        <div className="px-6 pb-6">
          <div className="flex items-end -mt-16">
            <div className="h-32 w-32 rounded-full border-4 border-white bg-white flex items-center justify-center shadow-lg">
              <span className="text-5xl font-bold text-blue-600">
                {employee.name.charAt(0)}
              </span>
            </div>
            <div className="ml-6 flex-1">
              <h2 className="text-2xl font-bold text-gray-900 mt-16">
                {employee.name}
              </h2>
              <p className="text-gray-600">{employee.jobTitle}</p>
            </div>
            <button className="mt-16 px-4 py-2 text-sm font-medium text-gray-700 bg-white border border-gray-300 rounded-md hover:bg-gray-50">
              Edit Profile
            </button>
          </div>
        </div>
      </div>

      {/* Personal Information */}
      <div className="bg-white shadow rounded-lg">
        <div className="px-6 py-5 border-b border-gray-200">
          <h2 className="text-lg font-medium text-gray-900">Personal Information</h2>
        </div>
        <div className="px-6 py-6 grid grid-cols-1 md:grid-cols-2 gap-6">
          <div className="flex items-start space-x-3">
            <Mail className="h-5 w-5 text-gray-400 mt-0.5" />
            <div>
              <p className="text-sm font-medium text-gray-500">Email</p>
              <p className="mt-1 text-sm text-gray-900">{employee.email}</p>
            </div>
          </div>

          <div className="flex items-start space-x-3">
            <Phone className="h-5 w-5 text-gray-400 mt-0.5" />
            <div>
              <p className="text-sm font-medium text-gray-500">Phone</p>
              <p className="mt-1 text-sm text-gray-900">{employee.phone}</p>
            </div>
          </div>

          <div className="flex items-start space-x-3">
            <MapPin className="h-5 w-5 text-gray-400 mt-0.5" />
            <div>
              <p className="text-sm font-medium text-gray-500">Location</p>
              <p className="mt-1 text-sm text-gray-900">{employee.location}</p>
            </div>
          </div>

          <div className="flex items-start space-x-3">
            <MapPin className="h-5 w-5 text-gray-400 mt-0.5" />
            <div>
              <p className="text-sm font-medium text-gray-500">Address</p>
              <p className="mt-1 text-sm text-gray-900">{employee.address}</p>
            </div>
          </div>
        </div>
      </div>

      {/* Employment Information */}
      <div className="bg-white shadow rounded-lg">
        <div className="px-6 py-5 border-b border-gray-200">
          <h2 className="text-lg font-medium text-gray-900">Employment Information</h2>
        </div>
        <div className="px-6 py-6 grid grid-cols-1 md:grid-cols-2 gap-6">
          <div className="flex items-start space-x-3">
            <Briefcase className="h-5 w-5 text-gray-400 mt-0.5" />
            <div>
              <p className="text-sm font-medium text-gray-500">Employee ID</p>
              <p className="mt-1 text-sm text-gray-900">{employee.employeeId}</p>
            </div>
          </div>

          <div className="flex items-start space-x-3">
            <Calendar className="h-5 w-5 text-gray-400 mt-0.5" />
            <div>
              <p className="text-sm font-medium text-gray-500">Hire Date</p>
              <p className="mt-1 text-sm text-gray-900">
                {new Date(employee.hireDate).toLocaleDateString()}
              </p>
            </div>
          </div>

          <div className="flex items-start space-x-3">
            <Building2 className="h-5 w-5 text-gray-400 mt-0.5" />
            <div>
              <p className="text-sm font-medium text-gray-500">Department</p>
              <p className="mt-1 text-sm text-gray-900">{employee.department}</p>
            </div>
          </div>

          <div className="flex items-start space-x-3">
            <Building2 className="h-5 w-5 text-gray-400 mt-0.5" />
            <div>
              <p className="text-sm font-medium text-gray-500">Organization</p>
              <p className="mt-1 text-sm text-gray-900">{employee.organization}</p>
            </div>
          </div>

          <div className="flex items-start space-x-3">
            <Briefcase className="h-5 w-5 text-gray-400 mt-0.5" />
            <div>
              <p className="text-sm font-medium text-gray-500">Reports To</p>
              <p className="mt-1 text-sm text-gray-900">{employee.manager}</p>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
