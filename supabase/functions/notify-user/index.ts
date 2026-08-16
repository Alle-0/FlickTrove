import { serve } from 'https://deno.land/std@0.168.0/http/server.ts'
import { initializeApp, cert } from 'npm:firebase-admin/app'
import { getFirestore } from 'npm:firebase-admin/firestore'
import { GoogleAuth } from 'npm:google-auth-library@9.0.0'

console.log("Supabase Edge Function for Firebase Notifications Started!")

let serviceAccount: any = null;
const serviceAccountKey = Deno.env.get('FIREBASE_SERVICE_ACCOUNT')
if (!serviceAccountKey) {
  console.error('FIREBASE_SERVICE_ACCOUNT environment variable is not set!')
} else {
  try {
    serviceAccount = JSON.parse(serviceAccountKey)
    initializeApp({
      credential: cert(serviceAccount)
    })
    getFirestore().settings({ preferRest: true })
    console.log("Firebase Admin initialized successfully.")
  } catch (error) {
    console.error("Error parsing FIREBASE_SERVICE_ACCOUNT:", error)
  }
}

const corsHeaders = {
  'Access-Control-Allow-Origin': '*',
  'Access-Control-Allow-Headers': 'authorization, x-client-info, apikey, content-type',
}

serve(async (req) => {
  // Handle CORS Preflight
  if (req.method === 'OPTIONS') {
    return new Response('ok', { headers: corsHeaders })
  }

  try {
    const authHeader = req.headers.get('Authorization')
    if (!authHeader || !authHeader.startsWith('Bearer ')) {
      return new Response(JSON.stringify({ error: 'Missing or invalid Authorization header' }), {
        status: 401,
        headers: { ...corsHeaders, 'Content-Type': 'application/json' },
      })
    }

    const idToken = authHeader.split('Bearer ')[1]
    
    // Verify the Firebase ID Token (Security check to prevent spam)
    const { getAuth } = await import('npm:firebase-admin/auth');
    let decodedToken;
    try {
      decodedToken = await getAuth().verifyIdToken(idToken);
    } catch (e) {
      return new Response(JSON.stringify({ error: 'Unauthorized: Invalid Firebase ID token' }), {
        status: 403,
        headers: { ...corsHeaders, 'Content-Type': 'application/json' },
      })
    }

    const callerUid = decodedToken.uid;

    const { targetUserId, title, body, titleLocKey, bodyLocKey, bodyLocArgs, mediaId, mediaType, mediaImage } = await req.json()

    if (!targetUserId || (!title && !titleLocKey) || (!body && !bodyLocKey)) {
      return new Response(JSON.stringify({ error: 'Missing required fields' }), {
        status: 400,
        headers: { ...corsHeaders, 'Content-Type': 'application/json' },
      })
    }
    
    // Don't send notifications to yourself
    if (callerUid === targetUserId) {
      return new Response(JSON.stringify({ success: true, message: 'Self notification skipped' }), {
        status: 200,
        headers: { ...corsHeaders, 'Content-Type': 'application/json' },
      })
    }

    // Fetch the target user's FCM token from Firestore
    const db = getFirestore()
    const tokenDoc = await db.collection('user_fcm_tokens').doc(targetUserId).get()
    
    if (!tokenDoc.exists) {
      return new Response(JSON.stringify({ error: 'Target user has no FCM token registered' }), {
        status: 404,
        headers: { ...corsHeaders, 'Content-Type': 'application/json' },
      })
    }

    const fcmToken = tokenDoc.data()?.fcmToken
    if (!fcmToken) {
      return new Response(JSON.stringify({ error: 'Target user FCM token is empty' }), {
        status: 404,
        headers: { ...corsHeaders, 'Content-Type': 'application/json' },
      })
    }

    // Send the push notification using standard REST fetch to bypass Deno node:http bug
    const auth = new GoogleAuth({
      credentials: {
        client_email: serviceAccount.client_email,
        private_key: serviceAccount.private_key,
      },
      scopes: ['https://www.googleapis.com/auth/firebase.messaging']
    })
    const client = await auth.getClient()
    const tokenResponse = await client.getAccessToken()

    const fcmUrl = `https://fcm.googleapis.com/v1/projects/${serviceAccount.project_id}/messages:send`
    const payload = {
      message: {
        token: fcmToken,
        data: {
          mediaId: String(mediaId || ''),
          mediaType: String(mediaType || ''),
          mediaImage: String(mediaImage || ''),
          title: title || '',
          body: body || '',
          titleLocKey: titleLocKey || '',
          bodyLocKey: bodyLocKey || '',
          click_action: 'FLICKTROVE_SOCIAL_NOTIFICATION'
        }
      }
    }

    const res = await fetch(fcmUrl, {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${tokenResponse.token}`,
        'Content-Type': 'application/json'
      },
      body: JSON.stringify(payload)
    })

    if (!res.ok) {
      const errorText = await res.text()
      throw new Error(`FCM API error: ${res.status} ${errorText}`)
    }

    const responseData = await res.json()

    return new Response(JSON.stringify({ success: true, messageId: responseData.name }), {
      headers: { ...corsHeaders, 'Content-Type': 'application/json' },
      status: 200,
    })

  } catch (error) {
    console.error("Error processing request:", error)
    return new Response(JSON.stringify({ error: error.message }), {
      headers: { ...corsHeaders, 'Content-Type': 'application/json' },
      status: 500,
    })
  }
})
