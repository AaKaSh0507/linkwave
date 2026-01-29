import type {
  ApiResponse,
  Contact,
  Message,
  OtpRequest,
  OtpVerify,
  User,
} from "./types";

// API base URL - connect to backend
const API_BASE_URL = process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080/api/v1";

// Helper to get CSRF token from cookie
function getCsrfToken(): string | null {
  const match = document.cookie.match(/XSRF-TOKEN=([^;]+)/);
  return match ? decodeURIComponent(match[1]) : null;
}

// Generic fetch wrapper
async function fetchApi<T>(
  endpoint: string,
  options: RequestInit = {},
  includeCsrf = false
): Promise<ApiResponse<T>> {
  try {
    const headers: Record<string, string> = {
      "Content-Type": "application/json",
      ...(options.headers as Record<string, string>),
    };

    // Include CSRF token for mutating requests
    if (includeCsrf) {
      const csrfToken = getCsrfToken();
      if (csrfToken) {
        headers["X-XSRF-TOKEN"] = csrfToken;
      }
    }

    const response = await fetch(`${API_BASE_URL}${endpoint}`, {
      ...options,
      headers,
      credentials: "include", // Include cookies for session
    });

    if (!response.ok) {
      const errorData = await response.json().catch(() => ({}));
      return {
        success: false,
        error: errorData.message || errorData.error || `HTTP error ${response.status}`,
      };
    }

    const data = await response.json();
    return { success: true, data };
  } catch (error) {
    return {
      success: false,
      error: error instanceof Error ? error.message : "Network error",
    };
  }
}

// Auth API - matches backend /api/v1/auth endpoints
export const authApi = {
  requestOtp: (data: OtpRequest) =>
    fetchApi<{ message: string }>("/auth/request-otp", {
      method: "POST",
      body: JSON.stringify({
        phoneNumber: data.phoneNumber,
        email: data.email,
      }),
    }),

  verifyOtp: (data: OtpVerify) =>
    fetchApi<{ authenticated: boolean; message: string }>("/auth/verify-otp", {
      method: "POST",
      body: JSON.stringify({
        phoneNumber: data.phoneNumber,
        otp: data.otp,
      }),
    }),

  logout: async () => {
    // First, fetch CSRF token (this sets the cookie)
    await fetch(`${API_BASE_URL}/auth/csrf`, { credentials: "include" });
    // Then logout with CSRF token
    return fetchApi<{ message: string }>("/auth/logout", {
      method: "POST",
    }, true);
  },

  // Get current user profile
  getSession: async (): Promise<ApiResponse<{ user: User }>> => {
    const result = await fetchApi<{ phoneNumber: string; authenticatedAt: string }>("/user/me");
    if (result.success && result.data) {
      // Transform backend response to User type
      const user: User = {
        id: result.data.phoneNumber, // Use phone number as ID for now
        phoneNumber: result.data.phoneNumber,
        displayName: undefined,
        avatarUrl: undefined,
        createdAt: result.data.authenticatedAt,
      };
      return { success: true, data: { user } };
    }
    return { success: false, error: result.error };
  },
};

// Contacts API - returns empty array if backend endpoint not available
export const contactsApi = {
  getContacts: async (): Promise<ApiResponse<Contact[]>> => {
    const result = await fetchApi<Contact[]>("/contacts");
    // Return empty array if endpoint doesn't exist yet
    if (!result.success && result.error?.includes("404")) {
      return { success: true, data: [] };
    }
    return result;
  },

  searchUsers: (query: string) =>
    fetchApi<User[]>(`/contacts/search?q=${encodeURIComponent(query)}`),
};

// Messages API - returns empty array if backend endpoint not available
export const messagesApi = {
  getMessages: async (recipientId: string, before?: string): Promise<ApiResponse<Message[]>> => {
    const params = new URLSearchParams();
    if (before) params.set("before", before);
    const result = await fetchApi<Message[]>(
      `/messages/${recipientId}${params.toString() ? `?${params}` : ""}`
    );
    // Return empty array if endpoint doesn't exist yet
    if (!result.success && result.error?.includes("404")) {
      return { success: true, data: [] };
    }
    return result;
  },

  sendReadReceipt: (messageIds: string[]) =>
    fetchApi<void>("/read-receipt", {
      method: "POST",
      body: JSON.stringify({ messageIds }),
    }, true),
};
