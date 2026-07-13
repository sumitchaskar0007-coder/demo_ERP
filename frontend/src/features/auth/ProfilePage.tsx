import { zodResolver } from "@hookform/resolvers/zod";
import { Building2, ImagePlus, Mail, Pencil, Phone, RefreshCw, ShieldCheck } from "lucide-react";
import { useEffect, useState } from "react";
import { useForm } from "react-hook-form";
import { toast } from "sonner";
import { z } from "zod";
import { Badge, StatusBadge } from "@/components/common/Badge";
import { Button } from "@/components/common/Button";
import { Card } from "@/components/common/Card";
import { Input } from "@/components/common/Input";
import { Loader } from "@/components/common/Loader";
import { Modal } from "@/components/common/Modal";
import { Textarea } from "@/components/common/Textarea";
import { initials } from "@/lib/utils";
import { updateOwnProfileSchema } from "@/lib/validators";
import { useAuth } from "./authStore";

type ProfileForm = z.infer<typeof updateOwnProfileSchema>;

function ProfileField({ label, value }: { label: string; value: string }) {
  return <div><p className="mb-2 text-sm font-semibold text-slate-700">{label}</p><div className="flex min-h-12 items-center rounded-xl border border-slate-200 bg-[#fbfcfe] px-4 text-sm font-medium text-slate-700">{value || "—"}</div></div>;
}

export function ProfilePage() {
  const { user, refreshProfile, updateProfile, uploadProfilePhoto } = useAuth();
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [editing, setEditing] = useState(false);
  const [photo, setPhoto] = useState<File | null>(null);
  const [photoPreview, setPhotoPreview] = useState<string | null>(null);
  const { register, reset, handleSubmit, formState: { errors, isSubmitting } } = useForm<ProfileForm>({ resolver: zodResolver(updateOwnProfileSchema) });

  useEffect(() => { refreshProfile().finally(() => setLoading(false)); }, [refreshProfile]);
  useEffect(() => {
    if (!photo) { setPhotoPreview(null); return; }
    const preview = URL.createObjectURL(photo);
    setPhotoPreview(preview);
    return () => URL.revokeObjectURL(preview);
  }, [photo]);
  if (loading || !user) return <div className="page-container"><Loader label="Loading your profile…" /></div>;

  const primaryRole = user.roles[0]?.replaceAll("_", " ") || "User";
  const openEditor = () => {
    reset({ phone: user.phone || "", address: user.address || "", bio: user.bio || "" });
    setPhoto(null);
    setEditing(true);
  };
  const save = async (values: ProfileForm) => {
    try { await updateProfile(values); if (photo) await uploadProfilePhoto(photo); toast.success("Profile updated successfully"); setEditing(false); setPhoto(null); }
    catch { toast.error("Could not update profile"); }
  };
  const refresh = async () => { setRefreshing(true); try { await refreshProfile(); } finally { setRefreshing(false); } };
  const imageUrl = user.profileImageUrl?.startsWith("/")
    ? `${import.meta.env.VITE_API_BASE_URL || "http://localhost:8081"}${user.profileImageUrl}`
    : user.profileImageUrl;

  return (
    <div className="page-container pb-10">
      <div className="flex items-start justify-between gap-4 border-b border-slate-200 pb-6"><div><h1 className="text-2xl font-bold">Profile</h1><p className="mt-2 text-sm text-slate-400">Dashboard&nbsp;&nbsp;/&nbsp;&nbsp;Settings&nbsp;&nbsp;/&nbsp;&nbsp;<span className="font-semibold text-slate-700">Profile</span></p></div><div className="flex gap-2"><Button variant="secondary" onClick={refresh} loading={refreshing} className="h-11 px-3"><RefreshCw className="h-4 w-4" /></Button><Button onClick={openEditor} className="h-11"><Pencil className="h-4 w-4" />Edit Profile</Button></div></div>
      <div className="mt-6 grid items-start gap-6 xl:grid-cols-[310px_1fr]">
        <Card className="overflow-hidden"><div className="border-b px-6 py-5"><h2 className="font-bold">Personal Information</h2></div><div className="p-6 text-center">{imageUrl ? <img src={imageUrl} alt={user.fullName} className="mx-auto h-28 w-28 rounded-full object-cover shadow-lg" /> : <div className="mx-auto grid h-28 w-28 place-items-center rounded-full bg-gradient-to-br from-brand-500 to-indigo-700 text-3xl font-bold text-white shadow-lg">{initials(user.fullName)}</div>}<h3 className="mt-5 text-xl font-bold">{user.fullName}</h3><p className="mt-1 text-sm capitalize text-slate-500">{primaryRole.toLowerCase()}</p><div className="mt-4 flex flex-wrap justify-center gap-2"><StatusBadge status={user.status} />{user.roles.map((role) => <Badge key={role} tone="info">{role.replaceAll("_", " ")}</Badge>)}</div><div className="mt-6 border-t pt-6 text-left"><div className="flex gap-3"><Mail className="h-4 w-4 text-brand-600" /><p className="break-all text-sm font-medium">{user.email}</p></div><div className="mt-5 flex gap-3"><Phone className="h-4 w-4 text-brand-600" /><p className="text-sm font-medium">{user.phone || "Not provided"}</p></div><div className="mt-5 flex gap-3"><Building2 className="h-4 w-4 text-brand-600" /><p className="text-sm font-medium">{user.collegeName || "System-wide"}</p></div></div></div></Card>
        <div className="space-y-6">
          <Card className="overflow-hidden"><div className="erp-panel-header"><div><h2 className="font-bold">Identity Information</h2><p className="mt-1 text-xs text-slate-400">Important identity fields are permanently protected</p></div><span className="rounded-lg bg-slate-100 px-3 py-1.5 text-xs font-semibold text-slate-500">Locked</span></div><div className="grid gap-5 p-6 md:grid-cols-2"><ProfileField label="Full Name" value={user.fullName} /><ProfileField label="Email Address" value={user.email} /><ProfileField label="User ID" value={String(user.id)} /><ProfileField label="Primary Role" value={primaryRole} /><ProfileField label="College" value={user.collegeName || "System-wide access"} /><ProfileField label="Account Status" value={user.status} /></div></Card>
          <Card className="overflow-hidden"><div className="erp-panel-header"><div><h2 className="font-bold">Additional Profile</h2><p className="mt-1 text-xs text-slate-400">Information you can update</p></div><Button variant="secondary" onClick={openEditor}><Pencil className="h-4 w-4" />Edit</Button></div><div className="grid gap-5 p-6 md:grid-cols-2"><ProfileField label="Phone Number" value={user.phone || "Not provided"} /><ProfileField label="Profile Image" value={user.profileImageUrl ? "Custom image added" : "Not provided"} /><div className="md:col-span-2"><ProfileField label="Address" value={user.address || "Not provided"} /></div><div className="md:col-span-2"><ProfileField label="Short Bio" value={user.bio || "Not provided"} /></div></div></Card>
          <Card className="p-6"><div className="flex gap-4"><div className="grid h-11 w-11 shrink-0 place-items-center rounded-xl bg-emerald-50 text-emerald-600"><ShieldCheck className="h-5 w-5" /></div><div><p className="font-semibold">Protected account</p><p className="mt-1 text-sm leading-6 text-slate-500">Name, email, college, roles and status cannot be edited from the profile page.</p></div></div></Card>
        </div>
      </div>

      <Modal open={editing} onClose={() => { setEditing(false); setPhoto(null); }} title="Edit profile" description="Upload a photo and update your non-critical profile fields.">
        <form onSubmit={handleSubmit(save)} className="space-y-5">
          <div>
            <p className="mb-2 text-sm font-semibold text-slate-700">Profile photo</p>
            <label className="flex cursor-pointer items-center gap-4 rounded-xl border border-dashed border-brand-300 bg-brand-50/40 p-4 transition hover:border-brand-500 hover:bg-brand-50">
              {photoPreview || imageUrl ? <img src={photoPreview || imageUrl || undefined} alt="Profile preview" className="h-20 w-20 shrink-0 rounded-full object-cover shadow-sm" /> : <span className="grid h-20 w-20 shrink-0 place-items-center rounded-full bg-white text-brand-600 shadow-sm"><ImagePlus className="h-7 w-7" /></span>}
              <span><span className="block font-semibold text-brand-700">Choose photo</span><span className="mt-1 block text-xs leading-5 text-slate-500">JPG, PNG, or WebP. Maximum size 5 MB.</span>{photo && <span className="mt-1 block max-w-[260px] truncate text-xs font-medium text-slate-700">{photo.name}</span>}</span>
              <input type="file" accept="image/jpeg,image/png,image/webp" className="sr-only" onChange={(event) => {
                const selected = event.target.files?.[0] || null;
                if (selected && selected.size > 5 * 1024 * 1024) { toast.error("Photo must not exceed 5 MB"); event.target.value = ""; return; }
                setPhoto(selected);
              }} />
            </label>
          </div>
          <Input label="Phone number" error={errors.phone?.message} {...register("phone")} />
          <Textarea label="Address" error={errors.address?.message} {...register("address")} />
          <Textarea label="Short bio" error={errors.bio?.message} {...register("bio")} />
          <div className="flex justify-end gap-3 border-t pt-5"><Button type="button" variant="secondary" onClick={() => { setEditing(false); setPhoto(null); }}>Cancel</Button><Button type="submit" loading={isSubmitting}>Save profile</Button></div>
        </form>
      </Modal>
    </div>
  );
}
