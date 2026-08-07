import { FilePlus2, Pencil, Power, Save, X } from "lucide-react";
import { useCallback, useEffect, useState } from "react";
import { toast } from "sonner";
import { Button } from "@/components/common/Button";
import { Card } from "@/components/common/Card";
import { Input } from "@/components/common/Input";
import { Select } from "@/components/common/Select";
import * as api from "@/features/admissions/api";
import type { AdmissionDocumentRequirement } from "@/features/admissions/types";
import { searchDepartments } from "@/features/departments/api";
import type { Department } from "@/features/departments/types";
import { handleApiError } from "@/lib/handleApiError";

export function AdmissionDocumentSettingsPage() {
  const [items, setItems] = useState<AdmissionDocumentRequirement[]>([]);
  const [departments, setDepartments] = useState<Department[]>([]);
  const [departmentId, setDepartmentId] = useState<number | "">("");
  const [name, setName] = useState("");
  const [required, setRequired] = useState(true);
  const [editingId, setEditingId] = useState<number | null>(null);
  const [editingName, setEditingName] = useState("");
  const [editingRequired, setEditingRequired] = useState(false);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);

  const load = useCallback(async () => {
    if (!departmentId) {
      setItems([]);
      setLoading(false);
      return;
    }
    setLoading(true);
    try {
      setItems(await api.getAdmissionDocumentSettings(departmentId));
    } catch (error) {
      toast.error(handleApiError(error).message);
    } finally {
      setLoading(false);
    }
  }, [departmentId]);

  useEffect(() => {
    searchDepartments({ status: "ACTIVE", page: 0, size: 100, sortBy: "name", sortDir: "asc" })
      .then((result) => {
        setDepartments(result.content);
        setDepartmentId(result.content[0]?.id ?? "");
      })
      .catch((error) => toast.error(handleApiError(error).message));
  }, []);

  useEffect(() => {
    setEditingId(null);
    void load();
  }, [load]);

  const create = async (event: React.FormEvent) => {
    event.preventDefault();
    if (name.trim().length < 2) {
      toast.error("Enter a document name");
      return;
    }
    setSaving(true);
    try {
      if (!departmentId) return;
      await api.createAdmissionDocumentSetting(
        { documentName: name.trim(), required },
        departmentId,
      );
      setName("");
      setRequired(true);
      await load();
      toast.success("Admission document added");
    } catch (error) {
      toast.error(handleApiError(error).message);
    } finally {
      setSaving(false);
    }
  };

  const saveEdit = async (item: AdmissionDocumentRequirement) => {
    if (!departmentId) return;
    setSaving(true);
    try {
      await api.updateAdmissionDocumentSetting(
        item.id,
        {
          documentName: editingName.trim(),
          required: editingRequired,
        },
        departmentId,
      );
      setEditingId(null);
      await load();
      toast.success("Admission document updated");
    } catch (error) {
      toast.error(handleApiError(error).message);
    } finally {
      setSaving(false);
    }
  };

  const toggle = async (item: AdmissionDocumentRequirement) => {
    if (!departmentId) return;
    setSaving(true);
    try {
      await api.setAdmissionDocumentSettingActive(item.id, !item.active, departmentId);
      await load();
      toast.success(item.active ? "Document hidden from admission form" : "Document enabled");
    } catch (error) {
      toast.error(handleApiError(error).message);
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="page-container space-y-6">
      <div>
        <p className="text-xs font-bold uppercase tracking-[0.16em] text-blue-600">
          Admission configuration
        </p>
        <h1 className="mt-2 text-3xl font-black tracking-tight text-slate-950">
          Required documents
        </h1>
        <p className="mt-2 max-w-3xl text-sm text-slate-600">
          Configure which file inputs students see for each department. Uploads accept PDF, JPEG, or
          PNG files with a maximum size of 2 MB.
        </p>
      </div>

      <Card className="p-5">
        <Select
          label="Department"
          value={departmentId}
          onChange={(event) =>
            setDepartmentId(event.target.value ? Number(event.target.value) : "")
          }
          options={[
            { label: "Select department", value: "" },
            ...departments.map((department) => ({
              label: `${department.name} (${department.code})`,
              value: department.id,
            })),
          ]}
        />
        {!departments.length && (
          <p className="mt-2 text-sm text-amber-700">
            Create an active department before configuring admission documents.
          </p>
        )}
      </Card>

      <Card className="p-5">
        <form onSubmit={create} className="grid gap-4 md:grid-cols-[1fr_auto_auto] md:items-end">
          <Input
            label="Document name"
            maxLength={120}
            placeholder="Example: Anti-ragging undertaking"
            value={name}
            onChange={(event) => setName(event.target.value)}
          />
          <label className="flex h-11 items-center gap-2 rounded-xl border border-slate-200 px-4 text-sm font-semibold text-slate-700">
            <input
              type="checkbox"
              checked={required}
              onChange={(event) => setRequired(event.target.checked)}
            />
            Required
          </label>
          <Button type="submit" loading={saving}>
            <FilePlus2 className="h-4 w-4" />
            Add document
          </Button>
        </form>
      </Card>

      <Card className="overflow-hidden">
        <div className="border-b border-slate-100 px-5 py-4">
          <h2 className="font-bold text-slate-900">
            {departmentId
              ? `${departments.find((department) => department.id === departmentId)?.name ?? ""} admission documents`
              : "Admission form documents"}
          </h2>
          <p className="mt-1 text-xs text-slate-500">
            Disabled items are preserved for existing admissions but hidden from new edits.
          </p>
        </div>
        <div className="divide-y divide-slate-100">
          {loading && <p className="p-5 text-sm text-slate-500">Loading document settings…</p>}
          {!loading && !items.length && (
            <p className="p-5 text-sm text-slate-500">No admission documents configured.</p>
          )}
          {items.map((item) => {
            const editing = editingId === item.id;
            return (
              <div
                key={item.id}
                className="flex flex-col gap-3 px-5 py-4 md:flex-row md:items-center"
              >
                <div className="min-w-0 flex-1">
                  {editing ? (
                    <Input
                      maxLength={120}
                      value={editingName}
                      onChange={(event) => setEditingName(event.target.value)}
                    />
                  ) : (
                    <>
                      <p
                        className={item.active ? "font-semibold text-slate-900" : "text-slate-400"}
                      >
                        {item.documentName}
                      </p>
                      <p className="mt-1 text-xs text-slate-500">{item.documentKey}</p>
                    </>
                  )}
                </div>
                {editing ? (
                  <label className="flex items-center gap-2 text-sm font-semibold text-slate-700">
                    <input
                      type="checkbox"
                      checked={editingRequired}
                      onChange={(event) => setEditingRequired(event.target.checked)}
                    />
                    Required
                  </label>
                ) : (
                  <span
                    className={`w-fit rounded-full px-2.5 py-1 text-xs font-bold ${
                      item.required ? "bg-rose-100 text-rose-700" : "bg-slate-100 text-slate-600"
                    }`}
                  >
                    {item.required ? "Required" : "Optional"}
                  </span>
                )}
                <div className="flex gap-2">
                  {editing ? (
                    <>
                      <Button type="button" loading={saving} onClick={() => void saveEdit(item)}>
                        <Save className="h-4 w-4" />
                        Save
                      </Button>
                      <Button type="button" variant="ghost" onClick={() => setEditingId(null)}>
                        <X className="h-4 w-4" />
                        Cancel
                      </Button>
                    </>
                  ) : (
                    <>
                      <Button
                        type="button"
                        variant="secondary"
                        onClick={() => {
                          setEditingId(item.id);
                          setEditingName(item.documentName);
                          setEditingRequired(item.required);
                        }}
                      >
                        <Pencil className="h-4 w-4" />
                        Edit
                      </Button>
                      <Button
                        type="button"
                        variant={item.active ? "danger" : "secondary"}
                        disabled={saving}
                        onClick={() => void toggle(item)}
                      >
                        <Power className="h-4 w-4" />
                        {item.active ? "Disable" : "Enable"}
                      </Button>
                    </>
                  )}
                </div>
              </div>
            );
          })}
        </div>
      </Card>
    </div>
  );
}
