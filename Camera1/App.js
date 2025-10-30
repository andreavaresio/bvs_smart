import React, {useCallback, useEffect, useMemo, useRef, useState} from 'react';
import {
  Alert,
  PermissionsAndroid,
  Platform,
  SafeAreaView,
  StatusBar,
  StyleSheet,
  Text,
  TextInput,
  TouchableOpacity,
  TouchableWithoutFeedback,
  View,
  Modal,
  ScrollView,
  Image,
  FlatList,
  ActivityIndicator,
} from 'react-native';
import {Linking, NativeModules} from 'react-native';
import {Camera, useCameraDevices} from 'react-native-vision-camera';
import {CameraRoll} from '@react-native-camera-roll/camera-roll';
import RNFS from 'react-native-fs';

// Centralized beehive list with GUID pairing
const BEEHIVES = [
  {id: 'IT-de11cede-1c18-4bd3-a383-a5349ac757a9', label: 'iPhone'},
  {id: 'IT-e6aa3784-6c9b-4116-af7b-228fa8bbe30d', label: 'motoroloa g82'},
  {id: 'IT-a2cb09a4-eac0-4d4e-906e-894e74eb4fcd', label: 'thinkphone'},
];

const version = '0.0.1';
const DEVICE_CAPABILITIES_ENDPOINT = 'https://www.meditazionearmoniosa.ovh/beevs-smart/api.php';
const DEVICE_CAPABILITIES_API_KEY = 'asdfjdsl567567sadfsda';

const ensureAndroidMediaPermission = async () => {
  if (Platform.OS !== 'android') {
    return true;
  }

  if (Platform.Version >= 33) {
    const statuses = await PermissionsAndroid.requestMultiple([
      PermissionsAndroid.PERMISSIONS.READ_MEDIA_IMAGES,
    ]);

    const hasImagesPermission =
      statuses[PermissionsAndroid.PERMISSIONS.READ_MEDIA_IMAGES] ===
      PermissionsAndroid.RESULTS.GRANTED;

    return hasImagesPermission;
  }

  const writePermission = await PermissionsAndroid.request(
    PermissionsAndroid.PERMISSIONS.WRITE_EXTERNAL_STORAGE,
  );

  if (writePermission === PermissionsAndroid.RESULTS.GRANTED) {
    return true;
  }

  const readPermission = await PermissionsAndroid.request(
    PermissionsAndroid.PERMISSIONS.READ_EXTERNAL_STORAGE,
  );

  return readPermission === PermissionsAndroid.RESULTS.GRANTED;
};

const IT_MONTHS = [
  'gennaio',
  'febbraio',
  'marzo',
  'aprile',
  'maggio',
  'giugno',
  'luglio',
  'agosto',
  'settembre',
  'ottobre',
  'novembre',
  'dicembre',
];

const formatItalianTimestamp = date => {
  try {
    return new Intl.DateTimeFormat('it-IT', {
      year: 'numeric',
      month: 'long',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit',
    }).format(date);
  } catch (e) {
    const day = String(date.getDate()).padStart(2, '0');
    const month = IT_MONTHS[date.getMonth()] ?? String(date.getMonth() + 1).padStart(2, '0');
    const year = date.getFullYear();
    const hours = String(date.getHours()).padStart(2, '0');
    const minutes = String(date.getMinutes()).padStart(2, '0');
    return `${day} ${month} ${year} ${hours}:${minutes}`;
  }
};

const guessFileName = (uri, fallback = 'photo.jpg') => {
  if (typeof uri !== 'string') {
    return fallback;
  }
  try {
    const sanitized = uri.split('?')[0];
    const candidate = sanitized.split('/').pop();
    if (!candidate || candidate.includes(':')) {
      return fallback;
    }
    const hasExtension = candidate.includes('.') && !candidate.endsWith('.');
    if (!hasExtension) {
      return fallback;
    }
    return candidate;
  } catch (e) {
    return fallback;
  }
};

const inferImageExtension = source => {
  if (typeof source !== 'string') {
    return '.jpg';
  }
  const lower = source.split('?')[0].toLowerCase();
  if (lower.endsWith('.jpeg')) return '.jpeg';
  if (lower.endsWith('.jpg')) return '.jpg';
  if (lower.endsWith('.png')) return '.png';
  if (lower.endsWith('.webp')) return '.webp';
  if (lower.endsWith('.heic')) return '.heic';
  if (lower.endsWith('.heif')) return '.heif';
  return '.jpg';
};

const sanitizeFileName = (value, fallback) => {
  const base = typeof value === 'string' && value.trim().length > 0 ? value.trim() : fallback;
  if (!base) {
    return 'photo.jpg';
  }
  return base.replace(/[^a-z0-9._-]/gi, '_');
};

const ensureImageFileName = (name, uri) => {
  const fallbackExtension = inferImageExtension(uri);
  const safeFallbackName = `photo_${Date.now()}${fallbackExtension}`;
  const sanitized = sanitizeFileName(name, safeFallbackName);
  const hasExtension = sanitized.includes('.') && !sanitized.endsWith('.');
  if (hasExtension) {
    return sanitized;
  }
  return `${sanitized}${fallbackExtension}`;
};

const resolveUploadUri = async (uri, filenameForCopy) => {
  if (!uri) return uri;
  if (typeof uri !== 'string') return uri;
  if (Platform.OS === 'android' && uri.startsWith('content://')) {
    try {
      const statResult = await RNFS.stat(uri);
      const originalPath = statResult?.originalFilepath || statResult?.path;
      if (originalPath) {
        if (originalPath.startsWith('file://')) {
          return originalPath;
        }
        if (originalPath.startsWith('/')) {
          return `file://${originalPath}`;
        }
      }
    } catch (statError) {
      console.log('[Upload] RNFS.stat failed for content URI', statError);
    }

    try {
      const safeName = sanitizeFileName(filenameForCopy, `photo_${Date.now()}.jpg`);
      const targetPath = `${RNFS.CachesDirectoryPath}/${Date.now()}_${safeName}`;
      await RNFS.copyFile(uri, targetPath);
      return `file://${targetPath}`;
    } catch (copyError) {
      console.log('[Upload] RNFS.copyFile fallback failed', copyError);
    }
  }
  return uri;
};

const guessMimeType = filename => {
  const name = (filename || '').toLowerCase();
  if (name.endsWith('.png')) return 'image/png';
  if (name.endsWith('.webp')) return 'image/webp';
  if (name.endsWith('.heic') || name.endsWith('.heif')) return 'image/heic';
  return 'image/jpeg';
};

const extractFormDataPartsForDebug = formData => {
  if (!formData) return [];
  if (typeof formData.getParts === 'function') {
    try {
      const parts = formData.getParts();
      if (Array.isArray(parts)) {
        return parts;
      }
    } catch (err) {
      console.log('[UploadDebug] formData.getParts failed', err?.message ?? err);
    }
  }
  const parts = formData?._parts;
  return Array.isArray(parts) ? parts : [];
};

const formatFormDataValueForDebug = value => {
  if (value && typeof value === 'object') {
    const formatted = {};
    if (value.uri) {
      formatted.uri = value.uri;
    }
    if (value.name) {
      formatted.name = value.name;
    }
    if (value.type) {
      formatted.type = value.type;
    }
    if ('size' in value && Number.isFinite(value.size)) {
      formatted.size = value.size;
    }
    if ('data' in value) {
      const dataString = String(value.data ?? '');
      formatted.dataPreview = `${dataString.slice(0, 60)}${dataString.length > 60 ? '…' : ''}`;
    }
    const remainingKeys = Object.keys(value).filter(
      key => !['uri', 'name', 'type', 'size', 'data'].includes(key),
    );
    remainingKeys.forEach(key => {
      formatted[key] = value[key];
    });
    return formatted;
  }
  return value;
};

const buildUploadRequestDebugDump = ({url, method, headers, formData}) => {
  const parts = extractFormDataPartsForDebug(formData).map((part, index) => {
    if (Array.isArray(part) && part.length >= 2) {
      const [name, value] = part;
      return {
        name: typeof name === 'string' ? name : `part_${index}`,
        value: formatFormDataValueForDebug(value),
      };
    }
    if (part && typeof part === 'object' && typeof part.fieldName === 'string') {
      return {
        name: part.fieldName,
        value: formatFormDataValueForDebug(part.string ?? part.data ?? part),
      };
    }
    return {
      name: `part_${index}`,
      value: formatFormDataValueForDebug(part),
    };
  });

  return {
    method: method ?? 'POST',
    url,
    headers: headers ?? {},
    formData: parts,
  };
};

const useCameraPermission = () => {
  const [hasPermission, setHasPermission] = useState(false);
  const [isLoading, setIsLoading] = useState(true);
  const [status, setStatus] = useState('not-determined');

  useEffect(() => {
    const requestPermission = async () => {
      try {
        const currentStatus = await Camera.getCameraPermissionStatus();
        setStatus(currentStatus);
        console.log('[CameraPermission] currentStatus =', currentStatus);
        if (currentStatus === 'granted') {
          setHasPermission(true);
          return;
        }

        const newStatus = await Camera.requestCameraPermission();
        setStatus(newStatus);
        console.log('[CameraPermission] newStatus =', newStatus);
        setHasPermission(newStatus === 'granted');
      } catch (error) {
        console.error('Errore nel controllo dei permessi della fotocamera', error);
        setHasPermission(false);
      } finally {
        setIsLoading(false);
      }
    };

    requestPermission();
  }, []);

  const retryRequest = useCallback(async () => {
    try {
      setIsLoading(true);
      const newStatus = await Camera.requestCameraPermission();
      setStatus(newStatus);
      console.log('[CameraPermission] retryStatus =', newStatus);
      setHasPermission(newStatus === 'granted');
    } catch (e) {
      console.error('Errore nella nuova richiesta dei permessi fotocamera', e);
    } finally {
      setIsLoading(false);
    }
  }, []);

  return {hasPermission, isLoading, status, retryRequest};
};

const getMaxResolutionFormat = device => {
  if (!device?.formats?.length) {
    return undefined;
  }

  const sortedFormats = [...device.formats].sort((a, b) => {
    const aPixels = (a.photoWidth ?? 0) * (a.photoHeight ?? 0);
    const bPixels = (b.photoWidth ?? 0) * (b.photoHeight ?? 0);
    return bPixels - aPixels;
  });

  return sortedFormats.find(
    format => (format.photoWidth ?? 0) > 0 && (format.photoHeight ?? 0) > 0,
  );
};

const getUniquePhotoSizesDesc = device => {
  if (!device?.formats?.length) return [];
  const set = new Set();
  const sizes = [];
  for (const f of device.formats) {
    const w = f.photoWidth ?? 0;
    const h = f.photoHeight ?? 0;
    if (w > 0 && h > 0) {
      const key = `${w}x${h}`;
      if (!set.has(key)) {
        set.add(key);
        sizes.push({w, h, mp: Math.round((w * h) / 1e6)});
      }
    }
  }
  sizes.sort((a, b) => b.w * b.h - a.w * a.h);
  return sizes;
};

const hashString = value => {
  const input = String(value ?? '');
  let hash = 0;
  for (let i = 0; i < input.length; i += 1) {
    hash = (Math.imul(31, hash) + input.charCodeAt(i)) >>> 0;
  }
  return hash.toString(16);
};

const slugify = value => {
  if (typeof value !== 'string') return 'device';
  const trimmed = value.trim().toLowerCase();
  const slug = trimmed.replace(/[^a-z0-9]+/g, '-').replace(/^-+|-+$/g, '');
  return slug || 'device';
};

const getPlatformMetadata = () => {
  const constants = Platform.constants ?? {};
  if (Platform.OS === 'android') {
    return {
      os: 'android',
      version: Platform.Version,
      brand: constants.Brand ?? null,
      manufacturer: constants.Manufacturer ?? null,
      model: constants.Model ?? null,
      fingerprint: constants.Fingerprint ?? null,
      serial: constants.Serial ?? null,
    };
  }
  return {
    os: Platform.OS,
    version: Platform.Version,
    systemName: constants.systemName ?? null,
    interfaceIdiom: constants.interfaceIdiom ?? null,
    osVersion: constants.osVersion ?? null,
  };
};

const buildDeviceIdentifier = ({platformMeta, devices}) => {
  const cameraPart = (Array.isArray(devices) ? devices : [])
    .map(device => `${device?.position ?? 'unknown'}:${device?.id ?? ''}`)
    .sort()
    .join('|');
  const raw = [
    platformMeta?.os,
    platformMeta?.version,
    platformMeta?.brand,
    platformMeta?.model,
    platformMeta?.manufacturer,
    platformMeta?.systemName,
    platformMeta?.interfaceIdiom,
    platformMeta?.osVersion,
    platformMeta?.fingerprint,
    platformMeta?.serial,
    cameraPart,
  ]
    .filter(Boolean)
    .join('|');
  const hash = hashString(raw);
  const suffixSource =
    platformMeta?.model || platformMeta?.systemName || platformMeta?.os || 'device';
  return `${platformMeta?.os ?? 'device'}-${slugify(suffixSource)}-${hash}`;
};

const normalizeCameraDevice = device => {
  if (!device) return null;
  return {
    id: device.id,
    name: device.name,
    position: device.position,
    hasFlash: !!device.hasFlash,
    hasTorch: !!device.hasTorch,
    supportsLowLightBoost: !!device.supportsLowLightBoost,
    supportsRawCapture: !!device.supportsRawCapture,
    isMultiCam: !!device.isMultiCam,
    minZoom: Number.isFinite(device.minZoom) ? device.minZoom : null,
    maxZoom: Number.isFinite(device.maxZoom) ? device.maxZoom : null,
    neutralZoom: Number.isFinite(device.neutralZoom) ? device.neutralZoom : null,
    minExposure: Number.isFinite(device.minExposure) ? device.minExposure : null,
    maxExposure: Number.isFinite(device.maxExposure) ? device.maxExposure : null,
    minFocusDistance: Number.isFinite(device.minFocusDistance)
      ? device.minFocusDistance
      : null,
    hardwareLevel: device.hardwareLevel ?? null,
    sensorOrientation: device.sensorOrientation ?? null,
    photoResolutions: getUniquePhotoSizesDesc(device).map(({w, h, mp}) => ({
      width: w,
      height: h,
      megapixels: mp,
    })),
  };
};

const buildSupportsLowLightBoostInfo = frontDevice => {
  if (!frontDevice) {
    return {
      isAvailable: false,
      isEnabledByDefault: null,
      canDisable: null,
      inference: 'front camera not found',
    };
  }
  const supportsLowLightBoost = !!frontDevice.supportsLowLightBoost;
  return {
    isAvailable: supportsLowLightBoost,
    isEnabledByDefault: supportsLowLightBoost ? null : false,
    canDisable: supportsLowLightBoost,
    inference: 'derived from supportsLowLightBoost property',
  };
};

const buildDeviceCapabilitiesPayload = async () => {
  try {
    const devices = await Camera.getAvailableCameraDevices();
    const cameraDevices = Array.isArray(devices) ? devices : [];
    const platformMeta = getPlatformMetadata();
    const normalizedCameras = cameraDevices
      .map(normalizeCameraDevice)
      .filter(Boolean);
    const frontDevice = cameraDevices.find(device => device?.position === 'front') ?? null;
    const frontCameraSummary = frontDevice
      ? normalizedCameras.find(camera => camera.id === frontDevice.id) ?? null
      : null;

    return {
      deviceid: buildDeviceIdentifier({platformMeta, devices: cameraDevices}),
      timestamp: new Date().toISOString(),
      appVersion: version,
      platform: platformMeta,
      supportsLowLightBoost: buildSupportsLowLightBoostInfo(frontDevice),
      frontCamera: frontCameraSummary,
      photoResolutions: frontCameraSummary?.photoResolutions ?? [],
      cameras: normalizedCameras,
    };
  } catch (error) {
    console.log('[DeviceCapabilities] failed to collect devices', error);
    return null;
  }
};

const InternalCameraScreen = ({onDone, selectedBeehive, scale}) => {
  const cameraRef = useRef(null);
  const {hasPermission: hasCameraPermission, isLoading, status, retryRequest} = useCameraPermission();
  const devices = useCameraDevices();
  useEffect(() => {
    if (devices) {
      console.log('[VisionCamera] devices:', devices.map(d => ({id: d.id, position: d.position, name: d.name, formats: d.formats?.length ?? 0})));
    }
  }, [devices]);
  const device = useMemo(() => {
    if (!devices || devices.length === 0) return null;
    return (
      devices.find(d => d.position === 'back') ||
      devices.find(d => d.position === 'external') ||
      devices.find(d => d.position === 'front') ||
      null
    );
  }, [devices]);

  const photoFormat = useMemo(() => getMaxResolutionFormat(device), [device]);
  const [isCameraReady, setIsCameraReady] = useState(false);
  const [isSaving, setIsSaving] = useState(false);
  const [lastPhotoInfo, setLastPhotoInfo] = useState(null);
  const beehiveLabel = selectedBeehive?.label ?? null;
  const beehiveId = selectedBeehive?.id ?? null;
  const normalizedScale = useMemo(() => (
    typeof scale === 'number' && Number.isFinite(scale) ? scale : 1.0
  ), [scale]);

  const handleCapture = useCallback(async () => {
    if (!cameraRef.current || !isCameraReady) {
      Alert.alert('Preparazione', 'La fotocamera non è pronta. Riprova.');
      return;
    }

    if (Platform.OS === 'android') {
      const hasGalleryPermission = await ensureAndroidMediaPermission();
      if (!hasGalleryPermission) {
        Alert.alert(
          'Permessi mancanti',
          'Concedi l\'accesso alla galleria per salvare le foto.',
        );
        return;
      }
    }

    try {
      setIsSaving(true);
      const photo = await cameraRef.current.takePhoto({
        qualityPrioritization: 'quality',
        enableShutterSound: true,
      });

      const filePath = photo?.path ?? '';
      if (!filePath) {
        throw new Error('Percorso della foto non trovato.');
      }

      const uri = filePath.startsWith('file://') ? filePath : `file://${filePath}`;

      await CameraRoll.saveAsset(uri, {
        type: 'photo',
        album: 'Camera',
      });
      setLastPhotoInfo({
        width: photo?.width,
        height: photo?.height,
        path: uri,
        beehiveId,
        beehiveLabel,
        scale: normalizedScale,
      });
      if (onDone) {
        await onDone({
          path: uri,
          width: photo?.width,
          height: photo?.height,
          beehiveId: beehiveId || null,
          beehiveLabel: beehiveLabel || null,
          scale: normalizedScale,
        });
      }
    } catch (error) {
      console.error('Errore durante la cattura della foto', error);
      Alert.alert(
        'Errore',
        'Non è stato possibile scattare o salvare la foto. Riprova.',
      );
    } finally {
      setIsSaving(false);
    }
  }, [beehiveId, beehiveLabel, isCameraReady, normalizedScale, onDone]);

  if (isLoading) {
    return (
      <SafeAreaView style={styles.centerContent}>
        <StatusBar barStyle="light-content" />
        <Text style={styles.infoText}>Verifica dei permessi...</Text>
      </SafeAreaView>
    );
  }

  if (!hasCameraPermission) {
    return (
      <SafeAreaView style={styles.centerContent}>
        <StatusBar barStyle="light-content" />
        <Text style={styles.infoText}>
          Permesso fotocamera negato ({status}). Concedilo dalle impostazioni o riprova.
        </Text>
        <View style={{height: 16}} />
        <View style={{flexDirection: 'row', gap: 12}}>
          <TouchableOpacity style={styles.secondaryButton} onPress={retryRequest}>
            <Text style={styles.secondaryButtonText}>Riprova</Text>
          </TouchableOpacity>
          <TouchableOpacity style={styles.secondaryButton} onPress={() => Linking.openSettings()}>
            <Text style={styles.secondaryButtonText}>Apri Impostazioni</Text>
          </TouchableOpacity>
        </View>
      </SafeAreaView>
    );
  }

  if (!device) {
    return (
      <SafeAreaView style={styles.centerContent}>
        <StatusBar barStyle="light-content" />
        <Text style={styles.infoText}>Caricamento dispositivo fotocamera...</Text>
      </SafeAreaView>
    );
  }

  return (
    <SafeAreaView style={styles.container}>
      <StatusBar translucent backgroundColor="transparent" barStyle="light-content" />
      <View style={styles.cameraContainer}>
        <Camera
          ref={cameraRef}
          style={StyleSheet.absoluteFill}
          device={device}
          isActive
          photo
          format={photoFormat}
          onInitialized={() => setIsCameraReady(true)}
          onError={error => {
            console.error('Camera initialization error', error);
            setIsCameraReady(false);
          }}
        />
        <View style={styles.overlay}>
          {beehiveLabel ? (
            <View style={styles.beehiveBadge}>
              <Text style={styles.beehiveBadgeText}>{`Beehive: ${beehiveLabel}`}</Text>
            </View>
          ) : null}
          <Text style={styles.debugText}>
            {`Selezionata: ${photoFormat?.photoWidth ?? '?'}x${photoFormat?.photoHeight ?? '?'} (${device?.name ?? ''})`}
          </Text>
          <Text style={styles.debugText}>{`Scale: ${normalizedScale}`}</Text>
          <Text style={[styles.debugText, {opacity: 0.7}]}> 
            {(() => {
              const sizes = getUniquePhotoSizesDesc(device).slice(0, 3);
              return sizes.length
                ? `Top: ${sizes.map(s => s.w + 'x' + s.h).join(', ')}`
                : '';
            })()}
          </Text>
          <TouchableOpacity
            style={[styles.captureButton, isSaving && styles.captureButtonDisabled]}
            onPress={handleCapture}
            disabled={isSaving}
          >
            <Text style={styles.captureText}>
              {isSaving ? 'Salvataggio...' : 'Scatta'}
            </Text>
          </TouchableOpacity>
        </View>
      </View>
    </SafeAreaView>
  );
};

const SplashScreen = () => {
  return (
    <SafeAreaView style={styles.centerContent}>
      <StatusBar barStyle="light-content" />
      <Text style={styles.beeEmoji} accessibilityRole="image" accessibilityLabel="Bee">🐝</Text>
    </SafeAreaView>
  );
};

const UploadStatusModal = ({visible, message = 'Invio foto...'}) => (
  <Modal visible={visible} transparent animationType="fade" onRequestClose={() => {}}>
    <View style={styles.uploadOverlay}>
      <View style={styles.uploadCard}>
        <ActivityIndicator color="#ffd54f" size="large" />
        <View style={{height: 16}} />
        <Text style={styles.uploadText}>{message}</Text>
      </View>
    </View>
  </Modal>
);

const HomeScreen = ({onInternal, onExternal, selectedBeehive, scale, onUpdateSettings}) => {
  const [modalVisible, setModalVisible] = useState(false);
  const [formBeehiveId, setFormBeehiveId] = useState(selectedBeehive?.id ?? BEEHIVES[0]?.id ?? null);
  const [formScale, setFormScale] = useState(scale != null ? String(scale) : '1.0');

  useEffect(() => {
    if (modalVisible) {
      setFormBeehiveId(selectedBeehive?.id ?? BEEHIVES[0]?.id ?? null);
      setFormScale(scale != null ? String(scale) : '1.0');
    }
  }, [modalVisible, selectedBeehive, scale]);

  const formattedScale = useMemo(() => {
    if (typeof scale === 'number' && Number.isFinite(scale)) {
      return scale.toFixed(2);
    }
    return '1.00';
  }, [scale]);

  const handleSaveSettings = useCallback(() => {
    if (!formBeehiveId) {
      Alert.alert('Selezione obbligatoria', 'Scegli un beehive prima di continuare.');
      return;
    }
    const normalizedScale = (formScale ?? '').replace(',', '.').trim();
    const parsedScale = Number.parseFloat(normalizedScale);
    if (!Number.isFinite(parsedScale)) {
      Alert.alert('Valore non valido', 'Inserisci un valore numerico per Scale (esempio: 1.0).');
      return;
    }
    onUpdateSettings?.({beehiveId: formBeehiveId, scale: parsedScale});
    setModalVisible(false);
  }, [formBeehiveId, formScale, onUpdateSettings]);

  return (
    <SafeAreaView style={styles.homeContainer}>
      <StatusBar barStyle="light-content" />
      <View style={styles.homeTopSection}>
        <Text style={styles.logoBee} accessibilityRole="image" accessibilityLabel="Bee logo">🐝</Text>
        <TouchableOpacity
          style={styles.homeStatusCard}
          onPress={() => setModalVisible(true)}
          activeOpacity={0.85}
          accessibilityRole="button"
          accessibilityLabel="Open settings"
        >
          <View style={styles.statusRow}>
            <Text style={styles.statusLabel}>Beehive</Text>
            <Text style={styles.statusValue}>{selectedBeehive?.label ?? 'Not set'}</Text>
          </View>
          <View style={styles.statusSeparator} />
          <View style={styles.statusRow}>
            <Text style={styles.statusLabel}>Scale</Text>
            <Text style={styles.statusValue}>{formattedScale}</Text>
          </View>
        </TouchableOpacity>
      </View>
      <View style={{height: 24}} />
      <TouchableOpacity
        style={styles.alertRow}
        onPress={() => {
          Alert.alert(
            'No advanced camera',
            'Per lanciare la fotocamera di Android da un\'app, puoi utilizzare un Intent con MediaStore.ACTION_IMAGE_CAPTURE, che apre l\'app fotocamera predefinita del dispositivo per scattare foto. Tuttavia, questa modalità standard tramite Intent non garantisce l\'accesso alle funzionalità avanzate come "Ultra Res" o la modalità foto notturna, che sono specifiche dell\'app fotocamera originale del produttore e non sono attivabili o accessibili tramite l\'Intent generico.'
          );
        }}
        accessibilityRole="button"
        accessibilityLabel="No advanced camera info"
      >
        <Text style={styles.alertIcon}>⚠️</Text>
        <Text style={styles.alertLabel}>No advanced camera</Text>
      </TouchableOpacity>
      <View style={{height: 16}} />
      <TouchableOpacity style={styles.primaryButton} onPress={onInternal}>
        <Text style={styles.primaryButtonText}>Internal Photo</Text>
      </TouchableOpacity>
      <View style={{height: 16}} />
      <TouchableOpacity style={styles.primaryButton} onPress={onExternal}>
        <Text style={styles.primaryButtonText}>External Photo</Text>
      </TouchableOpacity>
      <View style={{height: 16}} />
      <TouchableOpacity style={styles.primaryButton} onPress={() => onExternal?.('gallery')}>
        <Text style={styles.primaryButtonText}>Gallery</Text>
      </TouchableOpacity>
      <View style={styles.versionContainer} pointerEvents="none">
        <Text style={styles.versionText}>Version {version}</Text>
      </View>

      <Modal
        visible={modalVisible}
        transparent
        animationType="fade"
        onRequestClose={() => setModalVisible(false)}
      >
        <TouchableWithoutFeedback onPress={() => setModalVisible(false)}>
          <View style={styles.modalOverlay}>
            <TouchableWithoutFeedback onPress={() => {}}>
              <View style={styles.modalContent}>
                <Text style={styles.modalTitle}>Beehive Settings</Text>
                <Text style={styles.modalLabel}>Beehive</Text>
                <ScrollView style={styles.modalList}>
                  {BEEHIVES.map(({id, label}) => {
                    const isSelected = formBeehiveId === id;
                    return (
                      <TouchableOpacity
                        key={id}
                        style={[styles.modalItem, isSelected && styles.modalItemSelected]}
                        onPress={() => setFormBeehiveId(id)}
                        accessibilityRole="button"
                        accessibilityState={{selected: isSelected}}
                      >
                        <Text style={styles.modalItemText}>{label}</Text>
                      </TouchableOpacity>
                    );
                  })}
                </ScrollView>
                <View style={{height: 16}} />
                <Text style={styles.modalLabel}>Scale</Text>
                <TextInput
                  value={formScale}
                  onChangeText={setFormScale}
                  keyboardType={Platform.OS === 'ios' ? 'decimal-pad' : 'numeric'}
                  placeholder="1.0"
                  placeholderTextColor="rgba(255,255,255,0.4)"
                  style={styles.modalTextInput}
                  accessibilityLabel="Scale value"
                />
                <View style={styles.modalActions}>
                  <TouchableOpacity
                    style={[styles.secondaryButton, {flex: 1}]}
                    onPress={() => setModalVisible(false)}
                  >
                    <Text style={styles.secondaryButtonText}>Cancel</Text>
                  </TouchableOpacity>
                  <View style={{width: 12}} />
                  <TouchableOpacity
                    style={[styles.primaryButton, {flex: 1, height: 48, width: '100%'}]}
                    onPress={handleSaveSettings}
                  >
                    <Text style={styles.primaryButtonText}>Save</Text>
                  </TouchableOpacity>
                </View>
              </View>
            </TouchableWithoutFeedback>
          </View>
        </TouchableWithoutFeedback>
      </Modal>
    </SafeAreaView>
  );
};

const GalleryScreen = ({onCancel, onSelected}) => {
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [photos, setPhotos] = useState([]);

  const loadPhotos = useCallback(async () => {
    try {
      setLoading(true);
      setError(null);
      if (Platform.OS === 'android') {
        const ok = await ensureAndroidMediaPermission();
        if (!ok) {
          setError('Permesso galleria negato. Concedilo dalle impostazioni.');
          setPhotos([]);
          return;
        }
      }
      const res = await CameraRoll.getPhotos({
        first: 200,
        assetType: 'Photos',
      });
      const edges = Array.isArray(res?.edges) ? res.edges : [];
      setPhotos(edges);
    } catch (e) {
      console.error('Errore caricamento galleria', e);
      setError('Impossibile caricare le foto.');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    loadPhotos();
  }, [loadPhotos]);

  const renderItem = ({item}) => {
    const uri = item?.node?.image?.uri;
    if (!uri) return null;
    return (
      <TouchableOpacity
        style={styles.galleryItem}
        onPress={() => {
          const filename = item?.node?.image?.filename || uri.split('/').pop() || 'Unknown';
          onSelected?.({uri, filename});
        }}
        accessibilityRole="button"
        accessibilityLabel={`Select photo ${item?.node?.image?.filename || ''}`}
      >
        <Image source={{uri}} style={styles.galleryImage} resizeMode="cover" />
      </TouchableOpacity>
    );
  };

  return (
    <SafeAreaView style={styles.container}>
      <StatusBar barStyle="light-content" />
      <View style={styles.galleryHeader}>
        <TouchableOpacity style={styles.secondaryButton} onPress={onCancel}>
          <Text style={styles.secondaryButtonText}>Back</Text>
        </TouchableOpacity>
        <Text style={[styles.primaryButtonText, {color: '#fff'}]}>Gallery</Text>
        <View style={{width: 86}} />
      </View>
      {loading ? (
        <View style={[styles.centerContent, {backgroundColor: '#000'}]}>
          <ActivityIndicator color="#ffd54f" size="large" />
          <View style={{height: 12}} />
          <Text style={styles.infoText}>Caricamento foto...</Text>
        </View>
      ) : error ? (
        <View style={[styles.centerContent, {backgroundColor: '#000'}]}>
          <Text style={styles.infoText}>{error}</Text>
          <View style={{height: 12}} />
          <View style={{flexDirection: 'row', gap: 12}}>
            <TouchableOpacity style={styles.secondaryButton} onPress={loadPhotos}>
              <Text style={styles.secondaryButtonText}>Riprova</Text>
            </TouchableOpacity>
            <TouchableOpacity style={styles.secondaryButton} onPress={() => Linking.openSettings()}>
              <Text style={styles.secondaryButtonText}>Impostazioni</Text>
            </TouchableOpacity>
          </View>
        </View>
      ) : (
        <FlatList
          data={photos}
          keyExtractor={(item, index) => item?.node?.image?.uri || String(index)}
          renderItem={renderItem}
          numColumns={3}
          contentContainerStyle={styles.galleryList}
        />
      )}
    </SafeAreaView>
  );
};

const App = () => {
  const [screen, setScreen] = useState('splash');
  const [selectedBeehiveId, setSelectedBeehiveId] = useState(BEEHIVES[0]?.id ?? null);
  const [scale, setScale] = useState(1.0);
  const [uploadState, setUploadState] = useState({visible: false, message: ''});
  const selectedBeehive = useMemo(
    () => BEEHIVES.find(b => b.id === selectedBeehiveId) ?? null,
    [selectedBeehiveId],
  );

  useEffect(() => {
    let isCancelled = false;

    const syncDeviceCapabilities = async () => {
      const payload = await buildDeviceCapabilitiesPayload();
      if (!payload || isCancelled) {
        return;
      }

      const hasKey = DEVICE_CAPABILITIES_API_KEY && DEVICE_CAPABILITIES_API_KEY.length > 0;
      const url = hasKey
        ? `${DEVICE_CAPABILITIES_ENDPOINT}?api-key=${encodeURIComponent(DEVICE_CAPABILITIES_API_KEY)}`
        : DEVICE_CAPABILITIES_ENDPOINT;

      try {
        const response = await fetch(url, {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
          },
          body: JSON.stringify(payload),
        });

        const text = await response.text();
        console.log('[DeviceCapabilities] response', {
          status: response.status,
          bodyPreview: text.slice(0, 200),
        });
      } catch (error) {
        console.error('[DeviceCapabilities] failed to send', error);
      }
    };

    syncDeviceCapabilities();

    return () => {
      isCancelled = true;
    };
  }, []);

  const handleUpdateSettings = useCallback(({beehiveId, scale: nextScale}) => {
    setSelectedBeehiveId(beehiveId);
    const normalized = Number.isFinite(nextScale) ? nextScale : 1.0;
    setScale(normalized);
  }, []);

  useEffect(() => {
    if (screen === 'splash') {
      const t = setTimeout(() => setScreen('home'), 3000);
      return () => clearTimeout(t);
    }
  }, [screen]);

  const handlePhotoUpload = useCallback(
    async (input, sourceLabel) => {
      const photoUri = input?.path || input?.uri;
      console.log('[Upload] resolved photoUri', photoUri);
      if (!photoUri) {
        Alert.alert('Nessuna foto', 'Non è stato possibile recuperare il file da inviare.');
        return false;
      }

      const rawFilename = input?.filename || guessFileName(photoUri, `photo_${Date.now()}.jpg`);
      const filename = ensureImageFileName(rawFilename, photoUri);
      const mimeType = guessMimeType(filename);
      const resolvedUri = await resolveUploadUri(photoUri, filename);
      console.log('[Upload] effective file uri', resolvedUri);
      const now = new Date();
      const isoTimestamp = now.toISOString();
      const sanitizedTimestamp = isoTimestamp.replace(/:/g, '-');
      const datePart = isoTimestamp.slice(0, 10);
      const hours = String(now.getHours()).padStart(2, '0');
      const minutes = String(now.getMinutes()).padStart(2, '0');
      const beehiveId = input?.beehiveId || selectedBeehive?.id || '';
      const effectiveScale = Number.isFinite(input?.scale)
        ? input?.scale
        : Number.isFinite(scale)
          ? scale
          : 1.0;
      const scaleString = Number.isFinite(effectiveScale)
        ? effectiveScale.toFixed(2)
        : '1.00';
      const note = `Foto scattata il ${formatItalianTimestamp(now)}`;

      const formData = new FormData();
      formData.append('username', 'test@test.com');
      formData.append('password', 'Ap1sf3ro.123');
      formData.append('files[]', {
        uri: resolvedUri,
        type: mimeType,
        name: filename,
      });
      if (beehiveId) {
        formData.append('arniaId', beehiveId);
      }
      formData.append('note', note);
      formData.append('ScaleforConta', scaleString);
      formData.append('timestamp', sanitizedTimestamp);
      formData.append('GPS', '45.0352891,7.5168128');
      const daysOfStay = input?.daysOfStay ?? '0';
      formData.append('NumeroGGPermanenza', String(daysOfStay));
      formData.append('data_prelievo_data', datePart);
      formData.append('data_prelievo_time', `${hours}:${minutes}`);
      formData.append('tipo_misura', input?.measureType ?? 'CadutaNaturale');

      const uploadMessage = sourceLabel
        ? `Invio foto (${sourceLabel})...`
        : 'Invio foto in corso...';
      setUploadState({visible: true, message: uploadMessage});

      const uploadUrl = 'https://apisferoweb.it/api/v4/APIUploadImage';
      console.log('[Upload] ready to send', {
        sourceLabel,
        photoUri,
        uploadUri: resolvedUri,
        filename,
        mimeType,
        beehiveId,
        scaleString,
        daysOfStay,
      });

      try {
        console.log('[Upload] POST', uploadUrl);
        const requestHeaders = {
          Accept: 'application/json',
          'Content-Type': 'multipart/form-data',
        };
        const requestDump = buildUploadRequestDebugDump({
          url: uploadUrl,
          method: 'POST',
          headers: requestHeaders,
          formData,
        });
        console.log('[Upload] request dump', requestDump);

        const response = await fetch(uploadUrl, {
          method: 'POST',
          body: formData,
          headers: requestHeaders,
        });

        const rawText = await response.text();
        console.log('[Upload] response status', response.status);
        console.log('[Upload] raw response', rawText);
        if (!response.ok) {
          throw new Error(`HTTP ${response.status}: ${rawText}`);
        }

        let serverMessage = 'Immagine inviata correttamente.';
        try {
          const parsed = JSON.parse(rawText);
          if (parsed?.message) {
            serverMessage = parsed.message;
          }
        } catch (e) {
          console.log('[Upload] JSON parse failed', e?.message ?? e);
          if (rawText) {
            serverMessage = rawText;
          }
        }

        console.log('[Upload] server message', serverMessage);
        Alert.alert('Upload completato', serverMessage);
        return true;
      } catch (error) {
        console.error('Errore durante l\'upload della foto', error);
        console.log('[Upload] request failed', {message: error?.message, stack: error?.stack});
        Alert.alert(
          'Errore upload',
          `Non è stato possibile inviare la foto${sourceLabel ? ` (${sourceLabel})` : ''}.\nDettagli: ${error?.message ?? 'Errore sconosciuto'}`,
        );
        return false;
      } finally {
        setUploadState({visible: false, message: ''});
      }
    },
    [scale, selectedBeehive?.id],
  );

  const openExternal = useCallback(async (mode) => {
    if (mode === 'gallery') {
      setScreen('gallery');
      return;
    }
    if (Platform.OS !== 'android') {
      Alert.alert('Non supportato', 'External Photo è disponibile solo su Android.');
      return;
    }
    try {
      const {ExternalCamera} = NativeModules;
      if (!ExternalCamera || !ExternalCamera.openCamera) {
        Alert.alert('Non disponibile', 'Modulo ExternalCamera non disponibile.');
        return;
      }
      const uri = await ExternalCamera.openCamera();
      if (uri) {
        await handlePhotoUpload({uri}, 'fotocamera esterna');
      }
    } catch (e) {
      console.warn('External camera cancelled or failed', e);
    } finally {
      setScreen('home');
    }
  }, [handlePhotoUpload]);

  let content = null;

  if (screen === 'splash') {
    content = <SplashScreen />;
  } else if (screen === 'home') {
    content = (
      <HomeScreen
        onInternal={() => setScreen('internal')}
        onExternal={openExternal}
        selectedBeehive={selectedBeehive}
        scale={scale}
        onUpdateSettings={handleUpdateSettings}
      />
    );
  } else if (screen === 'internal') {
    content = (
      <InternalCameraScreen
        selectedBeehive={selectedBeehive}
        scale={scale}
        onDone={async result => {
          try {
            console.log('[PhotoResult]', result);
          } catch (e) {}
          await handlePhotoUpload(result, 'fotocamera interna');
          setScreen('home');
        }}
      />
    );
  } else if (screen === 'gallery') {
    content = (
      <GalleryScreen
        onCancel={() => setScreen('home')}
        onSelected={async ({uri, filename}) => {
          await handlePhotoUpload({uri, filename}, 'galleria');
          setScreen('home');
        }}
      />
    );
  }

  return (
    <>
      {content}
      <UploadStatusModal
        visible={uploadState.visible}
        message={uploadState.message || 'Invio foto in corso...'}
      />
    </>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#000',
  },
  cameraContainer: {
    flex: 1,
  },
  overlay: {
    ...StyleSheet.absoluteFillObject,
    justifyContent: 'flex-end',
    alignItems: 'center',
    paddingBottom: 48,
  },
  beehiveBadge: {
    position: 'absolute',
    top: 24,
    alignSelf: 'center',
    backgroundColor: 'rgba(0,0,0,0.5)',
    borderColor: '#ffd54f',
    borderWidth: 1,
    paddingHorizontal: 12,
    paddingVertical: 6,
    borderRadius: 12,
  },
  beehiveBadgeText: {
    color: '#ffd54f',
    fontSize: 14,
    fontWeight: '700',
  },
  debugText: {
    color: '#fff',
    fontSize: 12,
    marginBottom: 8,
  },
  captureButton: {
    width: 160,
    height: 56,
    borderRadius: 28,
    backgroundColor: 'rgba(255, 255, 255, 0.2)',
    borderWidth: 2,
    borderColor: '#fff',
    justifyContent: 'center',
    alignItems: 'center',
  },
  captureButtonDisabled: {
    opacity: 0.6,
  },
  captureText: {
    color: '#fff',
    fontSize: 18,
    fontWeight: '600',
  },
  centerContent: {
    flex: 1,
    backgroundColor: '#000',
    justifyContent: 'center',
    alignItems: 'center',
    padding: 24,
  },
  homeContainer: {
    flex: 1,
    backgroundColor: '#000',
    alignItems: 'center',
    padding: 24,
    position: 'relative',
  },
  homeTopSection: {
    width: '100%',
    alignItems: 'center',
  },
  logoBee: {
    fontSize: 96,
    opacity: 0.4,
    textAlign: 'center',
    marginBottom: 12,
  },
  homeStatusCard: {
    width: '100%',
    maxWidth: 360,
    alignSelf: 'center',
    minWidth: 260,
    paddingVertical: 16,
    paddingHorizontal: 20,
    borderRadius: 16,
    borderWidth: 1,
    borderColor: '#ffd54f',
    backgroundColor: 'rgba(255, 213, 79, 0.14)',
  },
  statusRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    width: '100%',
  },
  statusLabel: {
    color: '#ffd54f',
    fontSize: 14,
    fontWeight: '600',
  },
  statusValue: {
    color: '#fff',
    fontSize: 16,
    fontWeight: '700',
  },
  statusSeparator: {
    width: '100%',
    height: 1,
    backgroundColor: 'rgba(255, 213, 79, 0.35)',
    marginVertical: 12,
  },
  infoText: {
    color: '#fff',
    fontSize: 16,
    textAlign: 'center',
  },
  beeEmoji: {
    fontSize: 128,
    textAlign: 'center',
  },
  secondaryButton: {
    paddingHorizontal: 16,
    paddingVertical: 10,
    borderRadius: 8,
    borderWidth: 1,
    borderColor: '#fff',
    backgroundColor: 'rgba(255,255,255,0.1)'
  },
  secondaryButtonText: {
    color: '#fff',
    fontSize: 14,
    fontWeight: '600',
  },
  primaryButton: {
    width: 240,
    height: 72,
    borderRadius: 14,
    backgroundColor: '#ffd54f',
    justifyContent: 'center',
    alignItems: 'center',
  },
  primaryButtonText: {
    color: '#000',
    fontSize: 20,
    fontWeight: '700',
  },
  alertRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
    paddingHorizontal: 8,
    paddingVertical: 6,
    borderRadius: 8,
    backgroundColor: 'rgba(255, 213, 79, 0.12)',
    borderColor: '#ffd54f',
    borderWidth: 1,
  },
  alertIcon: {
    fontSize: 16,
    marginRight: 6,
  },
  alertLabel: {
    color: '#ffd54f',
    fontSize: 14,
    fontWeight: '700',
  },
  modalOverlay: {
    flex: 1,
    backgroundColor: 'rgba(0,0,0,0.6)',
    justifyContent: 'center',
    alignItems: 'center',
    padding: 24,
  },
  modalContent: {
    width: '100%',
    maxWidth: 360,
    backgroundColor: '#1a1a1a',
    borderRadius: 12,
    padding: 16,
  },
  modalTitle: {
    color: '#fff',
    fontSize: 18,
    fontWeight: '700',
    marginBottom: 12,
    textAlign: 'center',
  },
  modalLabel: {
    color: '#fff',
    fontSize: 14,
    fontWeight: '600',
    marginBottom: 8,
  },
  modalList: {
    maxHeight: 220,
  },
  modalItem: {
    paddingHorizontal: 12,
    paddingVertical: 12,
    borderRadius: 8,
    backgroundColor: '#2a2a2a',
    marginBottom: 8,
  },
  modalItemSelected: {
    borderWidth: 1,
    borderColor: '#ffd54f',
    backgroundColor: 'rgba(255, 213, 79, 0.2)',
  },
  modalItemText: {
    color: '#fff',
    fontSize: 16,
  },
  modalTextInput: {
    borderRadius: 8,
    borderWidth: 1,
    borderColor: '#3a3a3a',
    backgroundColor: '#1f1f1f',
    paddingHorizontal: 12,
    paddingVertical: 10,
    color: '#fff',
    fontSize: 16,
  },
  modalActions: {
    flexDirection: 'row',
    alignItems: 'center',
    marginTop: 24,
  },
  galleryHeader: {
    height: 56,
    backgroundColor: '#111',
    borderBottomWidth: 1,
    borderBottomColor: '#222',
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingHorizontal: 12,
  },
  galleryList: {
    padding: 2,
  },
  galleryItem: {
    flex: 1,
    aspectRatio: 1,
    margin: 2,
    backgroundColor: '#1a1a1a',
    borderRadius: 6,
    overflow: 'hidden',
  },
  galleryImage: {
    width: '100%',
    height: '100%',
  },
  uploadOverlay: {
    flex: 1,
    backgroundColor: 'rgba(0, 0, 0, 0.68)',
    justifyContent: 'center',
    alignItems: 'center',
    padding: 24,
  },
  uploadCard: {
    width: '100%',
    maxWidth: 260,
    backgroundColor: '#1c1c1c',
    borderRadius: 12,
    paddingVertical: 24,
    paddingHorizontal: 20,
    alignItems: 'center',
    borderWidth: 1,
    borderColor: '#ffd54f',
  },
  uploadText: {
    color: '#fff',
    fontSize: 16,
    textAlign: 'center',
  },
  versionContainer: {
    position: 'absolute',
    bottom: 24,
    alignSelf: 'center',
  },
  versionText: {
    color: 'rgba(255, 255, 255, 0.4)',
    fontSize: 12,
    textAlign: 'center',
  },
});

export default App;
