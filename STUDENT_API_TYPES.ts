// Student Module API Types
// Types TypeScript pour le frontend - Module Étudiant

// ==================== BASE TYPES ====================

export interface ApiResponse<T> {
  data?: T;
  error?: string;
  message?: string;
  timestamp?: string;
}

export interface PaginatedResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  last: boolean;
  empty: boolean;
}

// ==================== PROFILE & DASHBOARD ====================

export interface StudentProfile {
  id: string;
  nom: string;
  prenom: string;
  email: string;
  matricule: string;
  dateNaissance: string | null;
  telephone: string;
  adresse: string;
  filiere: string;
  niveau: string;
  photoUrl: string;
}

export interface AttendanceStats {
  tauxPresence: number;
  present: number;
  absent: number;
  retards: number;
}

export interface StudentDashboard {
  totalCredits: number;
  moyenneGenerale: number;
  coursEnCours: number;
  devoirsARendre: number;
  prochainsExamens: any[];
  statsPresence: AttendanceStats;
  notifications: any[];
}

export interface DigitalBadge {
  id: string;
  studentId: string;
  studentName: string;
  matricule: string;
  classe: string;
  qrPayload: string;
  token: string;
  minuteEpoch: number;
  expiresInSeconds: number;
  status: 'ACTIF' | 'INACTIF' | 'EXPIRE';
}

// ==================== ATTENDANCES ====================

export type AttendanceStatus = 'PRESENT' | 'ABSENT' | 'RETARD';
export type AttendanceMethod = 'QR_CODE' | 'MANUAL' | 'AUTO' | 'GPS';

export interface Attendance {
  id: string;
  date: string;
  status: {
    value: AttendanceStatus;
  };
  studentId: string;
  studentName: string;
  courseId: string;
}

export interface AttendanceRequest {
  type: AttendanceStatus;
  method: AttendanceMethod;
}

export interface AttendanceResponse {
  success: boolean;
  attendanceId: string;
  timestamp: string;
  status: string;
}

export interface QrValidationRequest {
  matricule: string;
  minuteEpoch: number;
  token: string;
}

export interface QrValidationResponse {
  valid: boolean;
  matricule: string;
  message: string;
  timestamp: string;
}

// ==================== LIVE SESSIONS ====================

export type SessionStatus = 'ACTIVE' | 'INACTIVE' | 'ENDED';
export type ReactionType = 'LIKE' | 'LOVE' | 'CLAP' | 'QUESTION';

export interface LiveSession {
  id: string;
  courseTitle: string;
  professorName: string;
  status: SessionStatus;
  activeUsers: number;
  link: string;
  time: string;
  reactions: Record<string, number>;
}

export interface ReactionRequest {
  type: ReactionType;
  emoji: string;
}

export interface ReactionResponse {
  success: boolean;
  sessionId: string;
  reaction: string;
}

export interface ChatMessage {
  id: string;
  text: string | null;
  userRole: string;
  user: string | null;
  userId: string;
  timestamp: string;
}

export interface ChatMessageRequest {
  content: string;
  senderId: string;
  senderName: string;
}

export interface ChatMessageResponse {
  id: string;
  success: boolean;
}

export interface ChatHistoryMessage {
  id: string;
  sessionId: string;
  userId: string;
  userName: string | null;
  userRole: string;
  text: string | null;
  createdAt: string;
  timestamp: string;
}

// ==================== GRADES ====================

export interface Grade {
  id: string;
  module: string;
  prof: string;
  ects: number;
  cc: number;
  examen: number;
  moyPromo: number;
  finalGrade: number;
  mention: string;
  validated: boolean;
}

export interface GradeReviewRequest {
  reason: string;
  comment: string;
}

export interface GradeReviewResponse {
  reason: string;
  gradeId: string;
  success: boolean;
  status: string;
}

// ==================== COURSES ====================

export type CourseStatus = 'EN_COURS' | 'TERMINE' | 'NON_COMMENCE';

export interface Course {
  id: string;
  titre: string;
  coefficient: number;
  progress: number;
  statut: {
    value: CourseStatus;
  };
  unites: any[];
}

export interface Homework {
  id: string;
  titre: string;
  points: string;
  statut: string;
  dateLimite: string;
}

export interface Quiz {
  options: string[];
  question: string;
  reponseCorrecte: number;
}

export interface Lesson {
  duree: string;
  titre: string;
  complete: boolean;
  estDerniereEnseignee?: boolean;
}

export interface Chapter {
  titre: string;
  lecons: Lesson[];
}

export interface Resource {
  type: string;
  titre: string;
  taille: string;
}

export interface CourseDetails {
  devoirs: Homework[];
  quizzes: Quiz[];
  imageUrl: string;
  chapitres: Chapter[];
  ressources: Resource[];
}

// ==================== HOMEWORKS ====================

export type HomeworkStatus = 'soumis' | 'en_cours' | 'a_rendre';

export interface StudentHomework {
  id: string;
  statut: HomeworkStatus;
  titre: string;
  deadline: string | null;
  prio: string;
  courseId: string;
  desc: string;
  progress: number;
  overdue: boolean;
}

export interface HomeworkSubmitResponse {
  homeworkId: string;
  fileName: string;
  size: number;
  success: boolean;
  status: string;
}

export interface HomeworkStartRequest {
  startTime: string;
}

export interface HomeworkStartResponse {
  homeworkId: string;
  success: boolean;
}

export interface HomeworkProgressRequest {
  progress: number;
  timeSpent: number;
}

export interface HomeworkProgressResponse {
  homeworkId: string;
  success: boolean;
}

// ==================== QUIZZES ====================

export interface QuizAnswer {
  questionId: string;
  answer: string;
}

export interface QuizSubmitRequest {
  quizId: string;
  answers: QuizAnswer[];
}

export interface QuizSubmitResponse {
  success: boolean;
  courseId: string;
  score: number;
  total: number;
  message: string;
  percentage: number;
}

// ==================== AI TUTOR ====================

export interface TutorChatRequest {
  message: string;
}

export interface TutorChatResponse {
  provider: string;
  available: boolean;
  text: string;
}

// ==================== NOTIFICATIONS ====================

export interface NotificationRequest {
  recipientId: string;
  content: string;
}

export interface NotificationResponse {
  provider: string;
  success: boolean;
  available: boolean;
  message: string;
}

export type ConfirmationType = 'EMAIL' | 'SMS';

export interface ConfirmationRequest {
  type: ConfirmationType;
  target: string;
}

export interface ConfirmationResponse {
  provider: string;
  success: boolean;
  available: boolean;
  message: string;
}

// ==================== ADMIN - PROFILE 360 ====================

export interface AdmissionDetails {
  dateInscription: string;
  lyceeOrigine: string;
  bacSerie: string;
  bacMention: string;
  statut: string;
  matricule: string;
  commissionAdmission: string;
  dateDecision: string;
}

export interface UE {
  status: string;
  coefficient: number;
  teacher: string;
  module: string;
  grade: number;
  ue: string;
}

export interface ConnectionLog {
  status: string;
  device: string;
  date: string;
  location: string;
}

export interface ActivityTrail {
  id: string;
  result: string;
  module: string;
  action: string;
  timestamp: string;
  actor: string;
}

export interface Timeline {
  status: string;
  desc: string;
  title: string;
  year: string;
}

export interface Payment {
  status: string;
  date: string;
  receiptNumber: string;
  method: string;
  amount: string;
  label: string;
}

export interface Extra {
  bourses: string;
  sante: string;
  messages: {
    text: string;
    sender: string;
    date: string;
  }[];
}

export interface StudentProfile360 {
  admissionDetails: AdmissionDetails;
  ueList: UE[];
  connectionLogs: ConnectionLog[];
  activityTrail: ActivityTrail[];
  timeline: Timeline[];
  payments: Payment[];
  extra: Extra;
}

export interface Profile360UpdateRequest {
  admissionDetails?: Partial<AdmissionDetails>;
  ueList?: UE[];
  connectionLogs?: ConnectionLog[];
  activityTrail?: ActivityTrail[];
  timeline?: Timeline[];
  payments?: Payment[];
  extra?: Partial<Extra>;
}

// ==================== ADMIN - DOCUMENTS ====================

export interface Document {
  id: string;
  name: string;
  size: string;
  url: string;
}

export interface DocumentRequest {
  name: string;
  size: string;
  url: string;
}

export interface DocumentResponse {
  documents: Document[];
}

// ==================== ADMIN - DIAGNOSTIC ====================

export interface DiagnosticRequest {
  studentId: string;
}

export interface DiagnosticResponse {
  text: string;
}

// ==================== ADMIN - SCHEDULE ====================

export interface ScheduleAssignRequest {
  courseId: string;
  day: string;
  time: string;
  room: string;
}

export interface ScheduleAssignResponse {
  courseId: string;
  day: string;
  time: string;
  room: string;
  success: boolean;
}

// ==================== API CLIENT ====================

export class StudentApiClient {
  private baseUrl: string;
  private token: string | null = null;

  constructor(baseUrl: string = 'http://localhost:8080/api') {
    this.baseUrl = baseUrl;
  }

  setToken(token: string) {
    this.token = token;
  }

  private getHeaders(): HeadersInit {
    const headers: HeadersInit = {
      'Content-Type': 'application/json',
    };

    if (this.token) {
      headers['Authorization'] = `Bearer ${this.token}`;
    }

    return headers;
  }

  private async request<T>(
    endpoint: string,
    options: RequestInit = {}
  ): Promise<T> {
    const url = `${this.baseUrl}${endpoint}`;
    const response = await fetch(url, {
      ...options,
      headers: {
        ...this.getHeaders(),
        ...options.headers,
      },
    });

    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`);
    }

    return response.json();
  }

  // ==================== PROFILE & DASHBOARD ====================

  async getProfile(): Promise<StudentProfile> {
    return this.request<StudentProfile>('/student/profile');
  }

  async getDashboard(): Promise<StudentDashboard> {
    return this.request<StudentDashboard>('/student/dashboard');
  }

  async getDigitalBadge(): Promise<DigitalBadge> {
    return this.request<DigitalBadge>('/student/digital-badge');
  }

  // ==================== ATTENDANCES ====================

  async getAttendances(page: number = 0, size: number = 10): Promise<PaginatedResponse<Attendance>> {
    return this.request<PaginatedResponse<Attendance>>(`/student/attendances?page=${page}&size=${size}`);
  }

  async selfCheckIn(request: AttendanceRequest): Promise<AttendanceResponse> {
    return this.request<AttendanceResponse>('/student/attendances', {
      method: 'POST',
      body: JSON.stringify(request),
    });
  }

  async validateQr(request: QrValidationRequest): Promise<QrValidationResponse> {
    return this.request<QrValidationResponse>('/student/qr-validate', {
      method: 'POST',
      body: JSON.stringify(request),
    });
  }

  // ==================== LIVE SESSIONS ====================

  async getLiveSessions(page: number = 0, size: number = 10): Promise<PaginatedResponse<LiveSession>> {
    return this.request<PaginatedResponse<LiveSession>>(`/student/live-sessions?page=${page}&size=${size}`);
  }

  async sendReaction(sessionId: string, request: ReactionRequest): Promise<ReactionResponse> {
    return this.request<ReactionResponse>(`/student/live-sessions/${sessionId}/reaction`, {
      method: 'POST',
      body: JSON.stringify(request),
    });
  }

  async getLiveSessionChat(sessionId: string): Promise<ChatMessage[]> {
    return this.request<ChatMessage[]>(`/student/live-sessions/${sessionId}/chat`);
  }

  async postToLiveSessionChat(sessionId: string, request: ChatMessageRequest): Promise<ChatMessageResponse> {
    return this.request<ChatMessageResponse>(`/student/live-sessions/${sessionId}/chat`, {
      method: 'POST',
      body: JSON.stringify(request),
    });
  }

  async getChatHistory(sessionId: string): Promise<ChatHistoryMessage[]> {
    return this.request<ChatHistoryMessage[]>(`/student/live-sessions/${sessionId}/chat-history`);
  }

  // ==================== GRADES ====================

  async getGrades(): Promise<Grade[]> {
    return this.request<Grade[]>('/student/grades');
  }

  async downloadGradesReport(): Promise<Blob> {
    const url = `${this.baseUrl}/student/grades/download`;
    const response = await fetch(url, {
      headers: this.getHeaders(),
    });

    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`);
    }

    return response.blob();
  }

  async requestGradeReview(gradeId: string, request: GradeReviewRequest): Promise<GradeReviewResponse> {
    return this.request<GradeReviewResponse>(`/student/grades/${gradeId}/review`, {
      method: 'POST',
      body: JSON.stringify(request),
    });
  }

  // ==================== COURSES ====================

  async getCourses(): Promise<Course[]> {
    return this.request<Course[]>('/student/courses');
  }

  async getCourseDetails(courseId: string): Promise<CourseDetails> {
    return this.request<CourseDetails>(`/student/courses/${courseId}/details`);
  }

  // ==================== HOMEWORKS ====================

  async getHomeworks(): Promise<StudentHomework[]> {
    return this.request<StudentHomework[]>('/student/homeworks');
  }

  async submitHomework(courseId: string, homeworkId: string, file: File, comment?: string): Promise<HomeworkSubmitResponse> {
    const formData = new FormData();
    formData.append('file', file);
    if (comment) {
      formData.append('comment', comment);
    }

    const url = `${this.baseUrl}/student/courses/${courseId}/homeworks/${homeworkId}/submit`;
    const response = await fetch(url, {
      method: 'POST',
      headers: this.token ? { 'Authorization': `Bearer ${this.token}` } : {},
      body: formData,
    });

    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`);
    }

    return response.json();
  }

  async startHomework(homeworkId: string, request: HomeworkStartRequest): Promise<HomeworkStartResponse> {
    return this.request<HomeworkStartResponse>(`/student/homeworks/${homeworkId}/start`, {
      method: 'POST',
      body: JSON.stringify(request),
    });
  }

  async updateHomeworkProgress(homeworkId: string, request: HomeworkProgressRequest): Promise<HomeworkProgressResponse> {
    return this.request<HomeworkProgressResponse>(`/student/homeworks/${homeworkId}/progress`, {
      method: 'POST',
      body: JSON.stringify(request),
    });
  }

  // ==================== QUIZZES ====================

  async submitQuiz(courseId: string, request: QuizSubmitRequest): Promise<QuizSubmitResponse> {
    return this.request<QuizSubmitResponse>(`/student/courses/${courseId}/quizzes/submit`, {
      method: 'POST',
      body: JSON.stringify(request),
    });
  }

  // ==================== AI TUTOR ====================

  async tutorChat(request: TutorChatRequest): Promise<TutorChatResponse> {
    return this.request<TutorChatResponse>('/student/tutor/chat', {
      method: 'POST',
      body: JSON.stringify(request),
    });
  }

  // ==================== NOTIFICATIONS ====================

  async sendNotification(request: NotificationRequest): Promise<NotificationResponse> {
    return this.request<NotificationResponse>('/student/send-notification', {
      method: 'POST',
      body: JSON.stringify(request),
    });
  }

  async requestConfirmation(request: ConfirmationRequest): Promise<ConfirmationResponse> {
    return this.request<ConfirmationResponse>('/student/request-confirmation', {
      method: 'POST',
      body: JSON.stringify(request),
    });
  }

  // ==================== ADMIN ENDPOINTS ====================

  async getStudentProfile360(studentId: string): Promise<StudentProfile360> {
    return this.request<StudentProfile360>(`/admin/students/${studentId}/profile360`);
  }

  async updateStudentProfile360(studentId: string, request: Profile360UpdateRequest): Promise<StudentProfile360> {
    return this.request<StudentProfile360>(`/admin/students/${studentId}/profile360`, {
      method: 'POST',
      body: JSON.stringify(request),
    });
  }

  async getStudentDocuments(studentId: string): Promise<Document[]> {
    return this.request<Document[]>(`/admin/students/${studentId}/documents`);
  }

  async addStudentDocument(studentId: string, request: DocumentRequest): Promise<DocumentResponse> {
    return this.request<DocumentResponse>(`/admin/students/${studentId}/documents`, {
      method: 'POST',
      body: JSON.stringify(request),
    });
  }

  async generateStudentDiagnostic(request: DiagnosticRequest): Promise<DiagnosticResponse> {
    return this.request<DiagnosticResponse>('/admin/student-diagnostic', {
      method: 'POST',
      body: JSON.stringify(request),
    });
  }

  async assignSchedule(request: ScheduleAssignRequest): Promise<ScheduleAssignResponse> {
    return this.request<ScheduleAssignResponse>('/admin/schedule/assign', {
      method: 'POST',
      body: JSON.stringify(request),
    });
  }
}

// ==================== EXAMPLE USAGE ====================

/*
// Initialize client
const apiClient = new StudentApiClient();

// Login and set token
async function login(email: string, password: string) {
  const response = await fetch('http://localhost:8080/api/auth/login-token', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ email, password }),
  });
  const data = await response.json();
  apiClient.setToken(data.token);
  return data;
}

// Example usage
async function exampleUsage() {
  try {
    // Login
    await login('student@ecole221.sn', 'password');

    // Get profile
    const profile = await apiClient.getProfile();
    console.log('Profile:', profile);

    // Get dashboard
    const dashboard = await apiClient.getDashboard();
    console.log('Dashboard:', dashboard);

    // Get attendances
    const attendances = await apiClient.getAttendances(0, 10);
    console.log('Attendances:', attendances);

    // Self check-in
    const checkIn = await apiClient.selfCheckIn({
      type: 'PRESENT',
      method: 'QR_CODE',
    });
    console.log('Check-in:', checkIn);

    // Get courses
    const courses = await apiClient.getCourses();
    console.log('Courses:', courses);

    // Submit homework
    const fileInput = document.getElementById('fileInput') as HTMLInputElement;
    if (fileInput.files && fileInput.files[0]) {
      const submitResult = await apiClient.submitHomework(
        'course-1',
        'hw-001',
        fileInput.files[0],
        'Mon devoir'
      );
      console.log('Homework submitted:', submitResult);
    }

  } catch (error) {
    console.error('Error:', error);
  }
}
*/
