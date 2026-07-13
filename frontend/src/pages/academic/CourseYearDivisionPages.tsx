import { zodResolver } from "@hookform/resolvers/zod";
import { Search } from "lucide-react";
import { useCallback, useEffect, useState } from "react";
import { useForm } from "react-hook-form";
import { Link, useNavigate, useParams } from "react-router-dom";
import { toast } from "sonner";
import { z } from "zod";
import { Badge, StatusBadge } from "@/components/common/Badge";
import { Button } from "@/components/common/Button";
import { Card } from "@/components/common/Card";
import { EmptyState } from "@/components/common/EmptyState";
import { Input } from "@/components/common/Input";
import { Loader } from "@/components/common/Loader";
import { Pagination } from "@/components/common/Pagination";
import { Select } from "@/components/common/Select";
import { DataTable, type Column } from "@/components/table/DataTable";
import * as academicApi from "@/features/academic/api";
import type { CourseYear, Division } from "@/features/academic/types";
import { searchDepartments } from "@/features/departments/api";
import type { Department } from "@/features/departments/types";
import type { StaffResponse } from "@/features/staff/types";
import { handleApiError } from "@/lib/handleApiError";
import { PAGE_SIZE, ROUTES, STATUS_OPTIONS } from "@/lib/constants";
import { courseYearSchema, divisionSchema } from "@/lib/validators";
import type { PageResponse } from "@/types/api";

const yearOptions = ["FIRST_YEAR", "SECOND_YEAR", "THIRD_YEAR", "FOURTH_YEAR", "FIFTH_YEAR"]
  .map((value) => ({ label: value.replaceAll("_", " "), value }));
const emptyPage = <T,>(): PageResponse<T> => ({ content: [], page: 0, size: PAGE_SIZE, totalElements: 0, totalPages: 0, last: true });

function useDepartments() {
  const [departments, setDepartments] = useState<Department[]>([]);
  useEffect(() => {
    searchDepartments({ status: "ACTIVE", page: 0, size: 100 })
      .then((result) => setDepartments(result.content))
      .catch((error) => toast.error(handleApiError(error).message));
  }, []);
  return departments;
}

export function CourseYearListPage() {
  const departments = useDepartments();
  const [result, setResult] = useState(emptyPage<CourseYear>());
  const [keyword, setKeyword] = useState(""); const [departmentId, setDepartmentId] = useState("");
  const [academicYear, setAcademicYear] = useState(""); const [status, setStatus] = useState("");
  const [page, setPage] = useState(0); const [loading, setLoading] = useState(true);
  const load = useCallback(async () => {
    setLoading(true);
    try { setResult(await academicApi.searchCourseYears({ keyword: keyword || undefined, departmentId: departmentId || undefined, academicYear: academicYear || undefined, status: status || undefined, page, size: PAGE_SIZE })); }
    catch (error) { toast.error(handleApiError(error).message); }
    finally { setLoading(false); }
  }, [academicYear, departmentId, keyword, page, status]);
  useEffect(() => { const timer = setTimeout(load, 250); return () => clearTimeout(timer); }, [load]);
  const toggle = async (row: CourseYear) => { try { await academicApi.setCourseYearStatus(row.id, row.status !== "ACTIVE"); toast.success("Course Year status updated"); await load(); } catch (error) { toast.error(handleApiError(error).message); } };
  const columns: Column<CourseYear>[] = [
    { key: "department", header: "Department", render: (r) => `${r.departmentCode} - ${r.departmentName}` },
    { key: "academicYear", header: "Academic Year", render: (r) => r.academicYear },
    { key: "year", header: "Year", render: (r) => r.yearName.replaceAll("_", " ") },
    { key: "displayName", header: "Display Name", render: (r) => r.displayName },
    { key: "code", header: "Code", render: (r) => <Badge>{r.code}</Badge> },
    { key: "status", header: "Status", render: (r) => <StatusBadge status={r.status} /> },
    { key: "divisions", header: "Divisions", render: (r) => r.divisionCount },
    { key: "actions", header: "Actions", render: (r) => <div className="flex gap-2"><Link to={`/principal/course-years/${r.id}/edit`}><Button variant="secondary">Edit</Button></Link><Button variant={r.status === "ACTIVE" ? "danger" : "secondary"} onClick={() => toggle(r)}>{r.status === "ACTIVE" ? "Deactivate" : "Activate"}</Button></div> },
  ];
  return <div className="page-container"><div className="flex items-start justify-between"><div><h1 className="page-title">Course Years</h1><p className="page-subtitle">Create each academic level inside a department.</p></div><Link to={ROUTES.createCourseYear}><Button>Create Course Year</Button></Link></div><Card className="mt-6"><div className="grid gap-3 border-b p-4 lg:grid-cols-4"><Input icon={<Search className="h-4 w-4" />} placeholder="Search..." value={keyword} onChange={(e) => { setKeyword(e.target.value); setPage(0); }} /><Select aria-label="Department" options={[{label:"All departments",value:""},...departments.map(d=>({label:d.name,value:d.id}))]} value={departmentId} onChange={e=>{setDepartmentId(e.target.value);setPage(0);}}/><Input placeholder="Academic year" value={academicYear} onChange={e=>{setAcademicYear(e.target.value);setPage(0);}}/><Select aria-label="Status" options={STATUS_OPTIONS} value={status} onChange={e=>{setStatus(e.target.value);setPage(0);}}/></div>{loading?<Loader label="Loading Course Years..."/>:result.content.length?<><DataTable columns={columns} data={result.content} rowKey={r=>r.id}/><div className="border-t p-4"><Pagination page={result.page} totalPages={result.totalPages} totalElements={result.totalElements} onChange={setPage}/></div></>:<EmptyState title="No Course Years" description="Create the first Course Year for a department."/>}</Card></div>;
}

type CourseYearValues = z.infer<typeof courseYearSchema>;
export function CourseYearFormPage() {
  const { id } = useParams(); const editId = id ? Number(id) : null; const navigate = useNavigate(); const departments = useDepartments();
  const { register, reset, handleSubmit, formState:{errors,isSubmitting} } = useForm<CourseYearValues>({ resolver:zodResolver(courseYearSchema), defaultValues:{departmentId:0,academicYear:"",yearName:"FIRST_YEAR",displayName:"",code:""} });
  useEffect(()=>{if(editId) academicApi.getCourseYear(editId).then(r=>reset({departmentId:r.departmentId,academicYear:r.academicYear,yearName:r.yearName,displayName:r.displayName,code:r.code})).catch(e=>toast.error(handleApiError(e).message));},[editId,reset]);
  const submit=async(v:CourseYearValues)=>{try{if(editId)await academicApi.updateCourseYear(editId,{displayName:v.displayName,code:v.code});else await academicApi.createCourseYear(v);toast.success(`Course Year ${editId?"updated":"created"}`);navigate(ROUTES.courseYears);}catch(e){toast.error(handleApiError(e).message);}};
  return <div className="page-container"><h1 className="page-title">{editId?"Edit":"Create"} Course Year</h1><p className="page-subtitle">Department and academic identity cannot be changed after creation.</p><Card className="mt-6 p-6"><form className="grid gap-4 md:grid-cols-2" onSubmit={handleSubmit(submit)}><Select label="Department" disabled={Boolean(editId)} options={[{label:"Select department",value:""},...departments.map(d=>({label:`${d.code} - ${d.name}`,value:d.id}))]} error={errors.departmentId?.message} {...register("departmentId")}/><Input label="Academic Year" disabled={Boolean(editId)} placeholder="2026-2027" error={errors.academicYear?.message} {...register("academicYear")}/><Select label="Year Name" disabled={Boolean(editId)} options={yearOptions} error={errors.yearName?.message} {...register("yearName")}/><Input label="Display Name" error={errors.displayName?.message} {...register("displayName")}/><Input label="Code" error={errors.code?.message} {...register("code")}/><div className="md:col-span-2"><Button type="submit" loading={isSubmitting}>Save Course Year</Button></div></form></Card></div>;
}

export function DivisionListPage() {
  const departments=useDepartments(); const [years,setYears]=useState<CourseYear[]>([]); const [result,setResult]=useState(emptyPage<Division>()); const [keyword,setKeyword]=useState(""); const [departmentId,setDepartmentId]=useState(""); const [courseYearId,setCourseYearId]=useState(""); const [status,setStatus]=useState(""); const [page,setPage]=useState(0); const [loading,setLoading]=useState(true); const [assigning,setAssigning]=useState<Division|null>(null); const [teachers,setTeachers]=useState<StaffResponse[]>([]); const [teacherId,setTeacherId]=useState("");
  useEffect(()=>{academicApi.searchCourseYears({departmentId:departmentId||undefined,status:"ACTIVE",page:0,size:100}).then(r=>setYears(r.content)).catch(()=>setYears([]));},[departmentId]);
  const load=useCallback(async()=>{setLoading(true);try{setResult(await academicApi.searchDivisions({keyword:keyword||undefined,departmentId:departmentId||undefined,courseYearId:courseYearId||undefined,status:status||undefined,page,size:PAGE_SIZE}));}catch(e){toast.error(handleApiError(e).message);}finally{setLoading(false);}},[courseYearId,departmentId,keyword,page,status]); useEffect(()=>{const t=setTimeout(load,250);return()=>clearTimeout(t);},[load]);
  const openAssign=async(row:Division)=>{setAssigning(row);setTeacherId("");try{setTeachers(await academicApi.eligibleClassTeachers(row.id));}catch(e){toast.error(handleApiError(e).message);}};
  const assign=async()=>{if(!assigning||!teacherId)return;try{await academicApi.assignDivisionClassTeacher(assigning.id,Number(teacherId));toast.success("Class Teacher assigned");setAssigning(null);await load();}catch(e){toast.error(handleApiError(e).message);}};
  const toggle=async(r:Division)=>{try{await academicApi.setDivisionStatus(r.id,r.status!=="ACTIVE");await load();}catch(e){toast.error(handleApiError(e).message);}};
  const remove=async(r:Division)=>{try{await academicApi.removeDivisionClassTeacher(r.id);toast.success("Class Teacher removed");await load();}catch(e){toast.error(handleApiError(e).message);}};
  const columns:Column<Division>[]=[{key:"department",header:"Department",render:r=>r.departmentName},{key:"courseYear",header:"Course Year",render:r=>r.courseYearDisplayName},{key:"division",header:"Division",render:r=>`${r.name} (${r.code})`},{key:"capacity",header:"Capacity",render:r=>r.capacity},{key:"teacher",header:"Class Teacher",render:r=>r.classTeacherName||"Not assigned"},{key:"status",header:"Status",render:r=><StatusBadge status={r.status}/>},{key:"actions",header:"Actions",render:r=><div className="flex flex-wrap gap-2"><Link to={`/principal/divisions/${r.id}/edit`}><Button variant="secondary">Edit</Button></Link><Button onClick={()=>openAssign(r)}>Assign</Button>{r.classTeacherId&&<Button variant="danger" onClick={()=>remove(r)}>Remove</Button>}<Button variant="secondary" onClick={()=>toggle(r)}>{r.status==="ACTIVE"?"Deactivate":"Activate"}</Button></div>}];
  return <div className="page-container"><div className="flex items-start justify-between"><div><h1 className="page-title">Divisions</h1><p className="page-subtitle">Manage divisions and their Class Teacher assignments.</p></div><Link to={ROUTES.createDivision}><Button>Create Division</Button></Link></div><Card className="mt-6"><div className="grid gap-3 border-b p-4 lg:grid-cols-4"><Input placeholder="Search divisions" value={keyword} onChange={e=>{setKeyword(e.target.value);setPage(0);}}/><Select aria-label="Department" options={[{label:"All departments",value:""},...departments.map(d=>({label:d.name,value:d.id}))]} value={departmentId} onChange={e=>{setDepartmentId(e.target.value);setCourseYearId("");setPage(0);}}/><Select aria-label="Course Year" options={[{label:"All Course Years",value:""},...years.map(y=>({label:y.displayName,value:y.id}))]} value={courseYearId} onChange={e=>{setCourseYearId(e.target.value);setPage(0);}}/><Select aria-label="Status" options={STATUS_OPTIONS} value={status} onChange={e=>{setStatus(e.target.value);setPage(0);}}/></div>{loading?<Loader label="Loading Divisions..."/>:result.content.length?<><DataTable columns={columns} data={result.content} rowKey={r=>r.id}/><div className="border-t p-4"><Pagination page={result.page} totalPages={result.totalPages} totalElements={result.totalElements} onChange={setPage}/></div></>:<EmptyState title="No Divisions" description="Create a Division under an active Course Year."/>}</Card>{assigning&&<Card className="mt-6 p-6"><h2 className="font-bold">Assign Class Teacher</h2><p className="mt-1 text-sm text-slate-500">{assigning.departmentName} → {assigning.courseYearDisplayName} → {assigning.name}</p><div className="mt-4 flex gap-3"><Select aria-label="Teacher" options={[{label:"Select active teacher",value:""},...teachers.map(t=>({label:`${t.fullName} (${t.staffType.replaceAll("_"," ")})`,value:t.id}))]} value={teacherId} onChange={e=>setTeacherId(e.target.value)}/><Button onClick={assign} disabled={!teacherId}>Assign</Button><Button variant="secondary" onClick={()=>setAssigning(null)}>Cancel</Button></div></Card>}</div>;
}

type DivisionValues=z.infer<typeof divisionSchema>;
export function DivisionFormPage(){const{id}=useParams();const editId=id?Number(id):null;const navigate=useNavigate();const departments=useDepartments();const[years,setYears]=useState<CourseYear[]>([]);const{register,watch,reset,handleSubmit,formState:{errors,isSubmitting}}=useForm<DivisionValues>({resolver:zodResolver(divisionSchema),defaultValues:{departmentId:0,courseYearId:0,name:"",code:"",capacity:60}});const departmentId=watch("departmentId");useEffect(()=>{if(departmentId)academicApi.searchCourseYears({departmentId,status:"ACTIVE",page:0,size:100}).then(r=>setYears(r.content));},[departmentId]);useEffect(()=>{if(editId)academicApi.getDivision(editId).then(r=>reset({departmentId:r.departmentId,courseYearId:r.courseYearId,name:r.name,code:r.code,capacity:r.capacity})).catch(e=>toast.error(handleApiError(e).message));},[editId,reset]);const submit=async(v:DivisionValues)=>{try{if(editId)await academicApi.updateDivision(editId,{name:v.name,code:v.code,capacity:v.capacity});else await academicApi.createDivision({courseYearId:v.courseYearId,name:v.name,code:v.code,capacity:v.capacity});toast.success(`Division ${editId?"updated":"created"}`);navigate(ROUTES.divisions);}catch(e){toast.error(handleApiError(e).message);}};return <div className="page-container"><h1 className="page-title">{editId?"Edit":"Create"} Division</h1><p className="page-subtitle">Choose an active Course Year inside the Department.</p><Card className="mt-6 p-6"><form className="grid gap-4 md:grid-cols-2" onSubmit={handleSubmit(submit)}><Select label="Department" disabled={Boolean(editId)} options={[{label:"Select department",value:""},...departments.map(d=>({label:d.name,value:d.id}))]} error={errors.departmentId?.message} {...register("departmentId")}/><Select label="Course Year" disabled={Boolean(editId)} options={[{label:"Select Course Year",value:""},...years.map(y=>({label:y.displayName,value:y.id}))]} error={errors.courseYearId?.message} {...register("courseYearId")}/><Input label="Division Name" error={errors.name?.message} {...register("name")}/><Input label="Division Code" error={errors.code?.message} {...register("code")}/><Input label="Capacity" type="number" error={errors.capacity?.message} {...register("capacity")}/><div className="md:col-span-2"><Button type="submit" loading={isSubmitting}>Save Division</Button></div></form></Card></div>}
