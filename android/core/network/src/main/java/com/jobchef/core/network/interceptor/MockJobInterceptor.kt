package com.jobchef.core.network.interceptor

import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody

class MockJobInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val path = request.url.encodedPath

        if (path.contains("/api/v1/jobs")) {
            val json = """
                {
                  "count": 5,
                  "results": [
                    {
                      "id": "mock_razorpay_01",
                      "title": "Senior Backend Engineer",
                      "company": {
                        "display_name": "Razorpay"
                      },
                      "location": {
                        "display_name": "Bengaluru"
                      },
                      "description": "Razorpay is seeking a Senior Backend Engineer to join our payments platform. Work with Go, Java, and highly scalable microservices architecture.",
                      "salary_min": 2200000.0,
                      "salary_max": 3000000.0,
                      "redirect_url": "https://razorpay.com/careers",
                      "created": "2026-06-13T12:00:00Z"
                    },
                    {
                      "id": "mock_cred_02",
                      "title": "Frontend Engineer",
                      "company": {
                        "display_name": "CRED"
                      },
                      "location": {
                        "display_name": "Bengaluru"
                      },
                      "description": "CRED is looking for a Frontend Engineer with React, React Native, and design-centric thinking to craft premium member experiences.",
                      "salary_min": 1800000.0,
                      "salary_max": 2500000.0,
                      "redirect_url": "https://cred.club/careers",
                      "created": "2026-06-13T12:00:00Z"
                    },
                    {
                      "id": "mock_zepto_03",
                      "title": "SRE / DevOps Engineer",
                      "company": {
                        "display_name": "Zepto"
                      },
                      "location": {
                        "display_name": "Mumbai"
                      },
                      "description": "Zepto is hiring a Site Reliability Engineer to optimize our 10-minute delivery infrastructure. Experience with Kubernetes and AWS is required.",
                      "salary_min": 2000000.0,
                      "salary_max": 2800000.0,
                      "redirect_url": "https://www.zeptonow.com/careers",
                      "created": "2026-06-13T12:00:00Z"
                    },
                    {
                      "id": "mock_phonepe_04",
                      "title": "Software Engineer (Android)",
                      "company": {
                        "display_name": "PhonePe"
                      },
                      "location": {
                        "display_name": "Bengaluru"
                      },
                      "description": "PhonePe is seeking an Android Developer to build high-performance SDKs for our payments ecosystem using Jetpack Compose and Kotlin.",
                      "salary_min": 1500000.0,
                      "salary_max": 2200000.0,
                      "redirect_url": "https://www.phonepe.com/careers",
                      "created": "2026-06-13T12:00:00Z"
                    },
                    {
                      "id": "mock_groww_05",
                      "title": "Backend Engineer (Kotlin/Java)",
                      "company": {
                        "display_name": "Groww"
                      },
                      "location": {
                        "display_name": "Bengaluru"
                      },
                      "description": "Groww is looking for a Backend Engineer to build robust financial systems. Work on high-concurrency systems using Kotlin and Spring Boot.",
                      "salary_min": 1600000.0,
                      "salary_max": 2400000.0,
                      "redirect_url": "https://groww.in/careers",
                      "created": "2026-06-13T12:00:00Z"
                    }
                  ]
                }
            """.trimIndent()

            return Response.Builder()
                .request(request)
                .protocol(Protocol.HTTP_2)
                .code(200)
                .message("OK")
                .body(json.toResponseBody("application/json".toMediaTypeOrNull()))
                .build()
        }

        return chain.proceed(request)
    }
}
