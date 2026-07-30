import { zodResolver } from "@hookform/resolvers/zod";
import {
  Building2,
  ImagePlus,
  Mail,
  MapPin,
  Pencil,
  Phone,
  RefreshCw,
  ShieldCheck,
  UserRound,
} from "lucide-react";
import { useEffect, useState } from "react";
import { useForm } from "react-hook-form";
import { toast } from "sonner";
import { z } from "zod";
import { Button } from "@/components/common/Button";
import { Card } from "@/components/common/Card";
import { Input } from "@/components/common/Input";
import { Loader } from "@/components/common/Loader";
import { Modal } from "@/components/common/Modal";
import { Textarea } from "@/components/common/Textarea";
import { initials } from "@/lib/utils";
import { updateOwnProfileSchema } from "@/lib/validators";
import { useAuth } from "@/features/auth/authStore";
import { API_BASE_URL } from "@/lib/apiClient";

type ProfileForm = z.infer<typeof updateOwnProfileSchema>;

function ProfileField({
  label,
  value,
  icon: Icon,
  wide = false,
}: {
  label: string;
  value: string;
  icon: typeof UserRound;
  wide?: boolean;
}) {
  return (
    <div
      className={`rounded-2xl border border-slate-200 bg-slate-50/80 p-4 transition-colors hover:border-brand-200 hover:bg-brand-50/30 ${
        wide ? "md:col-span-2" : ""
      }`}
    >
      <div className="flex items-start gap-3">
        <span className="grid h-10 w-10 shrink-0 place-items-center rounded-xl bg-white text-brand-600 shadow-sm ring-1 ring-slate-200">
          <Icon className="h-4 w-4" />
        </span>
        <div className="min-w-0">
          <p className="text-xs font-bold uppercase tracking-[0.12em] text-slate-400">{label}</p>
          <p className="mt-1 break-words text-sm font-semibold leading-6 text-slate-800">
            {value || "—"}
          </p>
        </div>
      </div>
    </div>
  );
}

export function ProfilePage() {
  const { user, refreshProfile, updateProfile, uploadProfilePhoto } = useAuth();
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [editing, setEditing] = useState(false);
  const [photo, setPhoto] = useState<File | null>(null);
  const [photoPreview, setPhotoPreview] = useState<string | null>(null);
  const {
    register,
    reset,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<ProfileForm>({ resolver: zodResolver(updateOwnProfileSchema) });

  useEffect(() => {
    refreshProfile().finally(() => setLoading(false));
  }, [refreshProfile]);
  useEffect(() => {
    if (!photo) {
      setPhotoPreview(null);
      return;
    }
    const preview = URL.createObjectURL(photo);
    setPhotoPreview(preview);
    return () => URL.revokeObjectURL(preview);
  }, [photo]);
  if (loading || !user)
    return (
      <div className="page-container">
        <Loader label="Loading your profile…" />
      </div>
    );

  const openEditor = () => {
    reset({ phone: user.phone || "", address: user.address || "", bio: user.bio || "" });
    setPhoto(null);
    setEditing(true);
  };
  const save = async (values: ProfileForm) => {
    try {
      await updateProfile(values);
      if (photo) await uploadProfilePhoto(photo);
      toast.success("Profile updated successfully");
      setEditing(false);
      setPhoto(null);
    } catch {
      toast.error("Could not update profile");
    }
  };
  const refresh = async () => {
    setRefreshing(true);
    try {
      await refreshProfile();
    } finally {
      setRefreshing(false);
    }
  };
  const imageUrl = user.profileImageUrl?.startsWith("/")
    ? `${API_BASE_URL}${user.profileImageUrl}`
    : user.profileImageUrl;

  return (
    <div className="page-container pb-10">
      <div className="flex flex-col gap-4 border-b border-slate-200 pb-6 sm:flex-row sm:items-start sm:justify-between">
        <div>
          <h1 className="text-2xl font-bold text-slate-900">My Profile</h1>
          <p className="mt-2 text-sm text-slate-400">
            Dashboard&nbsp;&nbsp;/&nbsp;&nbsp;Settings&nbsp;&nbsp;/&nbsp;&nbsp;
            <span className="font-semibold text-slate-700">Profile</span>
          </p>
        </div>
        <div className="flex w-full gap-2 sm:w-auto">
          <Button
            variant="secondary"
            onClick={refresh}
            loading={refreshing}
            className="h-11 px-3"
            aria-label="Refresh profile"
          >
            <RefreshCw className="h-4 w-4" />
          </Button>
          <Button onClick={openEditor} className="h-11 flex-1 sm:flex-none">
            <Pencil className="h-4 w-4" />
            Edit Profile
          </Button>
        </div>
      </div>
      <div className="mt-6 grid items-start gap-6 xl:grid-cols-[310px_1fr]">
        <Card className="overflow-hidden">
          <div className="h-24 bg-gradient-to-br from-brand-500 via-brand-600 to-indigo-700" />
          <div className="-mt-14 px-6 pb-6 text-center">
            <div className="mx-auto h-28 w-28 rounded-full bg-white p-1.5 shadow-xl ring-1 ring-slate-200">
              {imageUrl ? (
                <img
                  src={imageUrl}
                  alt={user.fullName}
                  className="h-full w-full rounded-full object-cover"
                />
              ) : (
                <div className="grid h-full w-full place-items-center rounded-full bg-gradient-to-br from-brand-100 to-indigo-100 text-3xl font-bold text-brand-700">
                  {initials(user.fullName)}
                </div>
              )}
            </div>
            <h2 className="mt-4 text-xl font-bold text-slate-900">{user.fullName}</h2>
            <p className="mt-1 text-sm text-slate-500">Personal profile</p>
            <div className="mt-6 border-t pt-6 text-left">
              <div className="flex items-start gap-3">
                <span className="grid h-8 w-8 shrink-0 place-items-center rounded-lg bg-brand-50 text-brand-600">
                  <Mail className="h-4 w-4" />
                </span>
                <div className="min-w-0">
                  <p className="text-xs font-semibold text-slate-400">Email</p>
                  <p className="mt-0.5 break-all text-sm font-medium text-slate-700">
                    {user.email}
                  </p>
                </div>
              </div>
              <div className="mt-4 flex items-start gap-3">
                <span className="grid h-8 w-8 shrink-0 place-items-center rounded-lg bg-brand-50 text-brand-600">
                  <Phone className="h-4 w-4" />
                </span>
                <div>
                  <p className="text-xs font-semibold text-slate-400">Phone</p>
                  <p className="mt-0.5 text-sm font-medium text-slate-700">
                    {user.phone || "Not provided"}
                  </p>
                </div>
              </div>
              <div className="mt-4 flex items-start gap-3">
                <span className="grid h-8 w-8 shrink-0 place-items-center rounded-lg bg-brand-50 text-brand-600">
                  <Building2 className="h-4 w-4" />
                </span>
                <div>
                  <p className="text-xs font-semibold text-slate-400">College</p>
                  <p className="mt-0.5 text-sm font-medium text-slate-700">
                    {user.collegeName || "System-wide"}
                  </p>
                </div>
              </div>
            </div>
            <Button variant="secondary" onClick={openEditor} className="mt-6 w-full">
              <Pencil className="h-4 w-4" />
              Update details
            </Button>
          </div>
        </Card>
        <div className="space-y-6">
          <Card className="overflow-hidden">
            <div className="erp-panel-header">
              <div>
                <h2 className="font-bold text-slate-900">Profile Overview</h2>
                <p className="mt-1 text-xs text-slate-400">
                  Your essential personal and college information
                </p>
              </div>
              <span className="rounded-full bg-emerald-50 px-3 py-1.5 text-xs font-semibold text-emerald-700">
                Up to date
              </span>
            </div>
            <div className="grid gap-5 p-6 md:grid-cols-2">
              <ProfileField label="Full Name" value={user.fullName} icon={UserRound} />
              <ProfileField label="Email Address" value={user.email} icon={Mail} />
              <ProfileField
                label="College"
                value={user.collegeName || "System-wide access"}
                icon={Building2}
                wide
              />
            </div>
          </Card>
          <Card className="overflow-hidden">
            <div className="erp-panel-header">
              <div>
                <h2 className="font-bold">Additional Profile</h2>
                <p className="mt-1 text-xs text-slate-400">Information you can update</p>
              </div>
              <Button variant="secondary" onClick={openEditor}>
                <Pencil className="h-4 w-4" />
                Edit
              </Button>
            </div>
            <div className="grid gap-5 p-6 md:grid-cols-2">
              <ProfileField
                label="Phone Number"
                value={user.phone || "Not provided"}
                icon={Phone}
              />
              <ProfileField
                label="Profile Photo"
                value={user.profileImageUrl ? "Photo added" : "Add a photo"}
                icon={ImagePlus}
              />
              <ProfileField
                label="Address"
                value={user.address || "Not provided"}
                icon={MapPin}
                wide
              />
              <ProfileField
                label="Short Bio"
                value={user.bio || "Tell people a little about yourself"}
                icon={UserRound}
                wide
              />
            </div>
          </Card>
          <Card className="border-brand-100 bg-gradient-to-r from-brand-50/70 to-indigo-50/60 p-6">
            <div className="flex gap-4">
              <div className="grid h-11 w-11 shrink-0 place-items-center rounded-xl bg-white text-brand-600 shadow-sm ring-1 ring-brand-100">
                <ShieldCheck className="h-5 w-5" />
              </div>
              <div>
                <p className="font-semibold text-slate-900">Your information stays protected</p>
                <p className="mt-1 text-sm leading-6 text-slate-500">
                  Your name, email, and college are managed centrally. You can update your photo,
                  phone number, address, and bio whenever needed.
                </p>
              </div>
            </div>
          </Card>
        </div>
      </div>

      <Modal
        open={editing}
        onClose={() => {
          setEditing(false);
          setPhoto(null);
        }}
        title="Edit profile"
        description="Upload a photo and update your non-critical profile fields."
      >
        <form onSubmit={handleSubmit(save)} className="space-y-5">
          <div>
            <p className="mb-2 text-sm font-semibold text-slate-700">Profile photo</p>
            <label className="flex cursor-pointer items-center gap-4 rounded-xl border border-dashed border-brand-300 bg-brand-50/40 p-4 transition hover:border-brand-500 hover:bg-brand-50">
              {photoPreview || imageUrl ? (
                <img
                  src={photoPreview || imageUrl || undefined}
                  alt="Profile preview"
                  className="h-20 w-20 shrink-0 rounded-full object-cover shadow-sm"
                />
              ) : (
                <span className="grid h-20 w-20 shrink-0 place-items-center rounded-full bg-white text-brand-600 shadow-sm">
                  <ImagePlus className="h-7 w-7" />
                </span>
              )}
              <span>
                <span className="block font-semibold text-brand-700">Choose photo</span>
                <span className="mt-1 block text-xs leading-5 text-slate-500">
                  JPG, PNG, or WebP. Maximum size 5 MB.
                </span>
                {photo && (
                  <span className="mt-1 block max-w-[260px] truncate text-xs font-medium text-slate-700">
                    {photo.name}
                  </span>
                )}
              </span>
              <input
                type="file"
                accept="image/jpeg,image/png,image/webp"
                className="sr-only"
                onChange={(event) => {
                  const selected = event.target.files?.[0] || null;
                  if (selected && selected.size > 5 * 1024 * 1024) {
                    toast.error("Photo must not exceed 5 MB");
                    event.target.value = "";
                    return;
                  }
                  setPhoto(selected);
                }}
              />
            </label>
          </div>
          <Input label="Phone number" error={errors.phone?.message} {...register("phone")} />
          <Textarea label="Address" error={errors.address?.message} {...register("address")} />
          <Textarea label="Short bio" error={errors.bio?.message} {...register("bio")} />
          <div className="flex justify-end gap-3 border-t pt-5">
            <Button
              type="button"
              variant="secondary"
              onClick={() => {
                setEditing(false);
                setPhoto(null);
              }}
            >
              Cancel
            </Button>
            <Button type="submit" loading={isSubmitting}>
              Save profile
            </Button>
          </div>
        </form>
      </Modal>
    </div>
  );
}
