import {
  createContext,
  useContext,
  useEffect,
  useMemo,
  useState,
  type PropsWithChildren,
} from "react";
import { useAuth } from "@/features/auth/authStore";
import { ROLES } from "@/lib/constants";
import { studentAcademicApi, type StudentAcademicAccess } from "./api";

interface StudentAcademicAccessContextValue {
  loading: boolean;
  access: StudentAcademicAccess | null;
  divisionAllocated: boolean;
}

const StudentAcademicAccessContext = createContext<StudentAcademicAccessContextValue>({
  loading: false,
  access: null,
  divisionAllocated: false,
});

export function StudentAcademicAccessProvider({ children }: PropsWithChildren) {
  const { isRole, user } = useAuth();
  const isStudent = isRole([ROLES.STUDENT]);
  const [loading, setLoading] = useState(isStudent);
  const [access, setAccess] = useState<StudentAcademicAccess | null>(null);

  useEffect(() => {
    let active = true;
    if (!isStudent) {
      setAccess(null);
      setLoading(false);
      return;
    }
    const refreshAccess = () => {
      setLoading(true);
      studentAcademicApi
        .accessState()
        .then((value) => active && setAccess(value))
        .catch(() => active && setAccess(null))
        .finally(() => active && setLoading(false));
    };
    const refreshWhenVisible = () => {
      if (document.visibilityState === "visible") refreshAccess();
    };

    refreshAccess();
    window.addEventListener("focus", refreshAccess);
    document.addEventListener("visibilitychange", refreshWhenVisible);
    return () => {
      active = false;
      window.removeEventListener("focus", refreshAccess);
      document.removeEventListener("visibilitychange", refreshWhenVisible);
    };
  }, [isStudent, user?.id]);

  const value = useMemo(
    () => ({ loading, access, divisionAllocated: Boolean(access?.divisionAllocated) }),
    [access, loading],
  );

  return (
    <StudentAcademicAccessContext.Provider value={value}>
      {children}
    </StudentAcademicAccessContext.Provider>
  );
}

export function useStudentAcademicAccess() {
  return useContext(StudentAcademicAccessContext);
}
